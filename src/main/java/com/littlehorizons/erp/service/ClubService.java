package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ClubFullException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;
import com.littlehorizons.erp.model.extracurricular.MembershipStatus;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.FileNames;
import com.littlehorizons.erp.util.InputValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/**
 * Clubs and memberships with capacity control.
 *
 * <p>Capacity is checked inside a transaction that first locks the club row
 * ({@code SELECT ... FOR UPDATE}); a second enrollment in the same club waits
 * until the first commits, so the count it reads is always current and the
 * limit cannot be exceeded by simultaneous enrollments.</p>
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

    /** Updates a club; the capacity cannot be set below the current active members. */
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

    public List<Club> findAll(boolean onlyActive) {
        return transactions.read(connection -> clubDao.findAll(connection, onlyActive));
    }

    public Club findById(int clubId) {
        return transactions.read(connection -> ServiceChecks.require(clubDao.findById(connection, clubId),
                "No existe un club con id " + clubId + "."));
    }

    /**
     * Enrolls an active student in an active club if a seat is free and the
     * student has no active membership in it.
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
                throw new ClubFullException("El club " + club.name() + " no tiene cupos disponibles ("
                        + members + " de " + club.capacity() + ").");
            }
            int id = clubDao.insertMembership(connection, new ClubMembership(null, studentId, student.fullName(),
                    clubId, club.name(), joinDate, MembershipStatus.ACTIVE, null, null));
            return clubDao.findMembershipById(connection, id).orElseThrow();
        });
    }

    /** Ends a membership; the record is kept as history. */
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

    public List<ClubMembership> findMembers(int clubId, boolean onlyActive) {
        return transactions.read(connection -> clubDao.findMembers(connection, clubId, onlyActive));
    }

    public List<ClubMembership> findByStudent(int studentId) {
        return transactions.read(connection -> clubDao.findByStudent(connection, studentId));
    }

    // ------------------------------------------------------ text file exercises

    /** Exercise 19: outcome of a bulk enrollment. {@code waitingListFile} is null when nobody waits. */
    public record BulkEnrollment(int enrolled, List<String> waiting, List<String> rejected, Path waitingListFile) {
    }

    /**
     * Exercise 19: reads one student document per line with {@link Scanner}
     * and enrolls them in order with {@link #enroll}, which locks the club and
     * checks the capacity. Once the club is full, the remaining students are
     * written to exports/lista_espera_&lt;club&gt;.txt with their position.
     */
    public BulkEnrollment enrollFromFile(int clubId, Path file, LocalDate joinDate) {
        Club club = findById(clubId);
        FileSupport.requireFile(file);
        int enrolled = 0;
        List<String> waiting = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8)) {
            int number = 0;
            while (scanner.hasNextLine()) {
                number++;
                String document = scanner.nextLine().replace("\uFEFF", "").strip();
                if (document.isEmpty() || document.startsWith("#")) {
                    continue;
                }
                Optional<Student> student = transactions.read(connection ->
                        studentDao.findByDocument(connection, document));
                if (student.isEmpty()) {
                    rejected.add(FileSupport.lineError(number, "no existe un estudiante con el documento "
                            + document + "."));
                    continue;
                }
                try {
                    enroll(student.get().id(), clubId, joinDate);
                    enrolled++;
                } catch (ClubFullException e) {
                    waiting.add(document + ";" + student.get().name().sortableName());
                } catch (ValidationException | BusinessRuleException e) {
                    rejected.add(FileSupport.lineError(number, e.getMessage()));
                }
            }
        } catch (IOException e) {
            AppLogger.logError(ClubService.class, "Could not read " + file, e);
            throw new BusinessRuleException("No fue posible leer el archivo " + file.getFileName() + ".");
        }
        Path waitingFile = null;
        if (!waiting.isEmpty()) {
            List<String> lines = new ArrayList<>();
            lines.add("# Lista de espera - " + club.name() + " - " + joinDate);
            lines.add("# posicion;documento;estudiante");
            for (int i = 0; i < waiting.size(); i++) {
                lines.add((i + 1) + ";" + waiting.get(i));
            }
            waitingFile = FileSupport.write(FileSupport.EXPORTS.resolve("lista_espera_" + FileNames.safe(club.name())
                    + ".txt"), lines, ClubService.class);
        }
        return new BulkEnrollment(enrolled, waiting, rejected, waitingFile);
    }

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
