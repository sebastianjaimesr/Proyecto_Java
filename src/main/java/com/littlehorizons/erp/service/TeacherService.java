package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.academic.WeekDay;
import com.littlehorizons.erp.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Profesores, sus asignaciones de materias y su horario semanal de clases, con
 * detección básica de conflictos de horario.
 */
public class TeacherService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ROOM_LENGTH = 50;
    private static final int SEARCH_LIMIT = 100;

    private final TransactionManager transactions;
    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;
    private final TeacherAssignmentDao assignmentDao;
    private final Clock clock;

    public TeacherService(TransactionManager transactions, TeacherDao teacherDao, SubjectDao subjectDao,
                          TeacherAssignmentDao assignmentDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // --------------------------------------------------------------- profesores

    public Teacher register(Teacher draft) {
        Teacher teacher = normalize(draft);
        validate(teacher);
        Teacher active = new Teacher(null, teacher.documentType(), teacher.documentNumber(), teacher.name(),
                teacher.email(), teacher.phone(), teacher.specialization(), teacher.hireDate(), TeacherStatus.ACTIVE);
        return transactions.inTransaction(connection -> {
            ensureUnique(connection, active, null);
            return active.withId(teacherDao.insert(connection, active));
        });
    }

    /** Actualiza datos y estado (Activo, Inactivo, Licencia). Los profesores nunca se borran. */
    public Teacher update(Teacher changes) {
        Objects.requireNonNull(changes.id(), "id");
        if (changes.status() == null) {
            throw new ValidationException("El estado del profesor es obligatorio.");
        }
        Teacher teacher = normalize(changes);
        validate(teacher);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(teacherDao.findById(connection, teacher.id()),
                    "No existe un profesor con id " + teacher.id() + ".");
            ensureUnique(connection, teacher, teacher.id());
            teacherDao.update(connection, teacher);
            return teacher;
        });
    }

    public Teacher findById(int teacherId) {
        return transactions.read(connection -> requireTeacher(connection, teacherId));
    }

    public List<Teacher> search(String text) {
        return transactions.read(connection -> teacherDao.search(connection, text, SEARCH_LIMIT));
    }

    // ------------------------------------------------------------ asignaciones

    /** Asigna un profesor activo a una materia activa para un año y una sección opcional. */
    public TeacherAssignment assign(int teacherId, int subjectId, int academicYear, String rawSection) {
        ServiceChecks.academicYear(academicYear, LocalDate.now(clock));
        String section = ServiceChecks.normalizeSection(rawSection);
        return transactions.inTransaction(connection -> {
            Teacher teacher = requireTeacher(connection, teacherId);
            if (teacher.status() != TeacherStatus.ACTIVE) {
                throw new BusinessRuleException("Solo se pueden asignar profesores en estado Activo.");
            }
            Subject subject = ServiceChecks.require(subjectDao.findById(connection, subjectId),
                    "No existe una materia con id " + subjectId + ".");
            if (!subject.active()) {
                throw new BusinessRuleException("La materia " + subject.name() + " está inactiva.");
            }
            if (assignmentDao.exists(connection, teacherId, subjectId, academicYear, section)) {
                throw new BusinessRuleException("El profesor ya tiene esa asignación para el año " + academicYear + ".");
            }
            int id = assignmentDao.insert(connection, new TeacherAssignment(null, teacherId, subjectId, academicYear,
                    section, teacher.fullName(), subject.name(), subject.gradeId()));
            return new TeacherAssignment(id, teacherId, subjectId, academicYear, section, teacher.fullName(),
                    subject.name(), subject.gradeId());
        });
    }

    public List<TeacherAssignment> findAssignments(int teacherId, int academicYear) {
        return transactions.read(connection -> {
            requireTeacher(connection, teacherId);
            return assignmentDao.findByTeacher(connection, teacherId, academicYear);
        });
    }

    public List<TeacherAssignment> findAssignmentsBySubject(int subjectId, int academicYear) {
        return transactions.read(connection -> assignmentDao.findBySubject(connection, subjectId, academicYear));
    }

    // -------------------------------------------------------------- horarios

    /**
     * Agrega una franja semanal de clase. Se rechaza si se cruza con otra franja del
     * mismo profesor, o del mismo grado y sección, en ese año.
     */
    public ClassSchedule addSchedule(int assignmentId, WeekDay day, LocalTime start, LocalTime end, String room) {
        validateTimes(day, start, end);
        String normalizedRoom = InputValidator.optionalText(room, "aula", MAX_ROOM_LENGTH);
        return transactions.inTransaction(connection -> {
            TeacherAssignment assignment = ServiceChecks.require(assignmentDao.findById(connection, assignmentId),
                    "No existe una asignación con id " + assignmentId + ".");
            Optional<ClassSchedule> teacherClash = assignmentDao
                    .findTeacherSchedules(connection, assignment.teacherId(), assignment.academicYear(), day)
                    .stream().filter(slot -> slot.overlaps(day, start, end)).findFirst();
            if (teacherClash.isPresent()) {
                throw new BusinessRuleException("Conflicto de horario del profesor con: "
                        + describe(teacherClash.get()) + ".");
            }
            Optional<ClassSchedule> groupClash = assignmentDao
                    .findGroupSchedules(connection, assignment.gradeId(), assignment.section(),
                            assignment.academicYear(), day)
                    .stream().filter(slot -> slot.overlaps(day, start, end)).findFirst();
            if (groupClash.isPresent()) {
                throw new BusinessRuleException("Conflicto de horario del grupo con: "
                        + describe(groupClash.get()) + ".");
            }
            ClassSchedule draft = new ClassSchedule(null, assignmentId, day, start, end, normalizedRoom, null);
            int id = assignmentDao.insertSchedule(connection, draft);
            return new ClassSchedule(id, assignmentId, day, start, end, normalizedRoom,
                    assignment.subjectName() + " - " + assignment.teacherName());
        });
    }

    public void removeSchedule(int scheduleId) {
        transactions.inTransactionVoid(connection -> assignmentDao.deleteSchedule(connection, scheduleId));
    }

    public List<ClassSchedule> findTimetable(int teacherId, int academicYear) {
        return transactions.read(connection -> {
            requireTeacher(connection, teacherId);
            return assignmentDao.findTeacherTimetable(connection, teacherId, academicYear);
        });
    }

    public List<ClassSchedule> findSchedulesByAssignment(int assignmentId) {
        return transactions.read(connection -> assignmentDao.findSchedulesByAssignment(connection, assignmentId));
    }

    static void validateTimes(WeekDay day, LocalTime start, LocalTime end) {
        if (day == null || start == null || end == null) {
            throw new ValidationException("El día, la hora de inicio y la hora de fin son obligatorios.");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (start.isBefore(ServiceChecks.SCHOOL_DAY_START) || end.isAfter(ServiceChecks.SCHOOL_DAY_END)) {
            throw new ValidationException("Las clases deben estar entre las "
                    + ServiceChecks.SCHOOL_DAY_START + " y las " + ServiceChecks.SCHOOL_DAY_END + ".");
        }
    }

    private static String describe(ClassSchedule slot) {
        return slot.description() + " (" + slot.day().dbValue() + " " + slot.startTime() + "-" + slot.endTime() + ")";
    }

    private Teacher requireTeacher(Connection connection, int teacherId) throws SQLException {
        return ServiceChecks.require(teacherDao.findById(connection, teacherId),
                "No existe un profesor con id " + teacherId + ".");
    }

    private void ensureUnique(Connection connection, Teacher teacher, Integer ownId) throws SQLException {
        Optional<Teacher> sameDocument = teacherDao.findByDocument(connection, teacher.documentNumber())
                .filter(existing -> !existing.id().equals(ownId));
        if (sameDocument.isPresent()) {
            throw new BusinessRuleException("Ya existe un profesor con el documento " + teacher.documentNumber() + ".");
        }
        Optional<Teacher> sameEmail = teacherDao.findByEmail(connection, teacher.email())
                .filter(existing -> !existing.id().equals(ownId));
        if (sameEmail.isPresent()) {
            throw new BusinessRuleException("El correo " + teacher.email() + " ya pertenece a otro profesor.");
        }
    }

    private static Teacher normalize(Teacher teacher) {
        return new Teacher(teacher.id(), teacher.documentType(),
                InputValidator.validateDocumentNumber(teacher.documentNumber()), teacher.name(),
                InputValidator.validateEmail(teacher.email(), "correo electrónico"),
                teacher.phone() == null || teacher.phone().isBlank() ? null : teacher.phone().strip(),
                teacher.specialization() == null || teacher.specialization().isBlank()
                        ? null : teacher.specialization().strip(),
                teacher.hireDate(), teacher.status());
    }

    private void validate(Teacher teacher) {
        if (teacher.documentType() == null) {
            throw new ValidationException("El tipo de documento es obligatorio.");
        }
        ServiceChecks.requireText(teacher.name().firstName(), "primer nombre", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(teacher.name().middleName(), "segundo nombre", MAX_NAME_LENGTH);
        ServiceChecks.requireText(teacher.name().lastName(), "primer apellido", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(teacher.name().secondLastName(), "segundo apellido", MAX_NAME_LENGTH);
        ServiceChecks.optionalPhone(teacher.phone(), "teléfono");
        ServiceChecks.optionalText(teacher.specialization(), "especialización", MAX_NAME_LENGTH);
        ServiceChecks.notInFuture(teacher.hireDate(), LocalDate.now(clock), "fecha de contratación");
    }
}
