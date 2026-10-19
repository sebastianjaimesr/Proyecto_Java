package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;
import com.littlehorizons.erp.model.extracurricular.MembershipStatus;
import com.littlehorizons.erp.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Clubes y membresías con control de cupo.
 *
 * <p>El cupo se revisa dentro de una transacción que primero bloquea la fila del club
 * ({@code SELECT ... FOR UPDATE}); una segunda inscripción al mismo club espera
 * a que la primera haga commit, así el conteo que lee siempre está actualizado y
 * el límite no se puede superar con inscripciones simultáneas.</p>
 */
public class ClubService {

    public static final int MAX_CAPACITY = 200;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_SCHEDULE_LENGTH = 100;

    private final TransactionManager transactions;
    private final ClubDao clubDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final Clock clock;

    public ClubService(TransactionManager transactions, ClubDao clubDao, StudentDao studentDao, TeacherDao teacherDao,
                       Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clubDao = Objects.requireNonNull(clubDao, "clubDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Crea un club después de validar sus datos, su supervisor y que el nombre no esté repetido. */
    public Club create(Club draft) {
        Club club = normalize(draft);
        return transactions.inTransaction(connection -> {
            checkSupervisor(connection, club.supervisorId());
            if (clubDao.findByName(connection, club.name()).isPresent()) {
                throw new BusinessRuleException("Ya existe un club llamado " + club.name() + ".");
            }
            int id = clubDao.insert(connection, club);
            return clubDao.findById(connection, id).orElseThrow();
        });
    }

    /** Actualiza un club; el cupo no puede quedar por debajo de los miembros activos actuales. */
    public Club update(Club changes) {
        Objects.requireNonNull(changes.id(), "id");
        Club club = normalize(changes);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(clubDao.lockById(connection, club.id()), "No existe un club con id " + club.id() + ".");
            checkSupervisor(connection, club.supervisorId());
            clubDao.findByName(connection, club.name())
                    .filter(existing -> !existing.id().equals(club.id()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("Ya existe otro club llamado " + club.name() + ".");
                    });
            int members = clubDao.countActiveMembers(connection, club.id());
            if (club.capacity() < members) {
                throw new BusinessRuleException("El cupo no puede ser menor que los " + members
                        + " miembros activos del club.");
            }
            clubDao.update(connection, club);
            return clubDao.findById(connection, club.id()).orElseThrow();
        });
    }

    /** Lista los clubes; {@code onlyActive} excluye los inactivos. */
    public List<Club> findAll(boolean onlyActive) {
        return transactions.read(connection -> clubDao.findAll(connection, onlyActive));
    }

    /** Busca un club por id o lanza un error si no existe. */
    public Club findById(int clubId) {
        return transactions.read(connection -> ServiceChecks.require(clubDao.findById(connection, clubId),
                "No existe un club con id " + clubId + "."));
    }

    /**
     * Inscribe a un estudiante activo en un club activo si hay cupo libre y el
     * estudiante no tiene ya una membresía activa en él.
     */
    public ClubMembership enroll(int studentId, int clubId, LocalDate joinDate) {
        ServiceChecks.notInFuture(joinDate, LocalDate.now(clock), "fecha de ingreso");
        return transactions.inTransaction(connection -> {
            Club club = ServiceChecks.require(clubDao.lockById(connection, clubId),
                    "No existe un club con id " + clubId + ".");
            if (!club.active()) {
                throw new BusinessRuleException("El club " + club.name() + " está inactivo.");
            }
            Student student = ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            if (student.status() != StudentStatus.ACTIVE) {
                throw new BusinessRuleException("Solo los estudiantes activos pueden inscribirse en clubes.");
            }
            if (clubDao.findActiveMembership(connection, studentId, clubId).isPresent()) {
                throw new BusinessRuleException(student.fullName() + " ya es miembro activo de " + club.name() + ".");
            }
            int members = clubDao.countActiveMembers(connection, clubId);
            if (members >= club.capacity()) {
                throw new BusinessRuleException("El club " + club.name() + " no tiene cupos disponibles ("
                        + members + " de " + club.capacity() + ").");
            }
            int id = clubDao.insertMembership(connection, new ClubMembership(null, studentId, student.fullName(),
                    clubId, club.name(), joinDate, MembershipStatus.ACTIVE, null, null));
            return clubDao.findMembershipById(connection, id).orElseThrow();
        });
    }

    /** Termina una membresía; el registro se conserva como historial. */
    public void withdraw(int membershipId, LocalDate date, String reason) {
        ServiceChecks.notInFuture(date, LocalDate.now(clock), "fecha de retiro");
        String cleanReason = InputValidator.optionalText(reason, "motivo", 1000);
        transactions.inTransactionVoid(connection -> {
            ClubMembership membership = ServiceChecks.require(clubDao.findMembershipById(connection, membershipId),
                    "No existe una membresía con id " + membershipId + ".");
            if (membership.status() != MembershipStatus.ACTIVE) {
                throw new BusinessRuleException("La membresía ya estaba retirada.");
            }
            if (date.isBefore(membership.joinDate())) {
                throw new ValidationException("La fecha de retiro no puede ser anterior al ingreso ("
                        + membership.joinDate() + ").");
            }
            clubDao.withdrawMembership(connection, membershipId, date, cleanReason);
        });
    }

    /** Miembros de un club; {@code onlyActive} excluye los retirados. */
    public List<ClubMembership> findMembers(int clubId, boolean onlyActive) {
        return transactions.read(connection -> clubDao.findMembers(connection, clubId, onlyActive));
    }

    /** Historial de clubes de un estudiante. */
    public List<ClubMembership> findByStudent(int studentId) {
        return transactions.read(connection -> clubDao.findByStudent(connection, studentId));
    }

    /** Si hay supervisor, verifica que el profesor exista y esté activo. */
    private void checkSupervisor(Connection connection, Integer teacherId) throws SQLException {
        if (teacherId == null) {
            return;
        }
        Teacher teacher = ServiceChecks.require(teacherDao.findById(connection, teacherId),
                "No existe un profesor con id " + teacherId + ".");
        if (teacher.status() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("El profesor encargado debe estar activo.");
        }
    }

    /** Valida y limpia los datos del club (nombre obligatorio, cupo dentro del rango permitido). */
    private static Club normalize(Club club) {
        String name = InputValidator.requireText(club.name(), "nombre del club", MAX_NAME_LENGTH);
        if (club.capacity() < 1 || club.capacity() > MAX_CAPACITY) {
            throw new ValidationException("El cupo máximo debe estar entre 1 y " + MAX_CAPACITY + ".");
        }
        return new Club(club.id(), name, InputValidator.optionalText(club.description(), "descripción", 2000),
                club.supervisorId(), club.supervisorName(), club.capacity(),
                InputValidator.optionalText(club.meetingSchedule(), "horario de reunión", MAX_SCHEDULE_LENGTH),
                club.active(), club.activeMembers());
    }
}
