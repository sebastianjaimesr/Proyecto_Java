package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectRegistrationDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfirmationRequiredException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Attendance;
import com.littlehorizons.erp.model.academic.AttendanceStatus;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.report.AbsenceReportRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Daily attendance per subject and the weekly absence report.
 *
 * <p>Approved rule (D-06): the analysis week is the ISO week, Monday to
 * Sunday, that contains the chosen date. Only "Ausente" counts as an
 * absence; "Tardanza" and "Excusa Médica" are reported apart. A student
 * appears when the absences of the week, added over all subjects, are more
 * than three.</p>
 */
public class AttendanceService {

    public static final int MAX_WEEKLY_ABSENCES = 3;
    private static final int MAX_NOTES_LENGTH = 1000;

    private final TransactionManager transactions;
    private final AttendanceDao attendanceDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;
    private final SubjectRegistrationDao registrationDao;
    private final TeacherAssignmentDao assignmentDao;
    private final Clock clock;

    public AttendanceService(TransactionManager transactions, AttendanceDao attendanceDao, StudentDao studentDao,
                             SubjectDao subjectDao, SubjectRegistrationDao registrationDao,
                             TeacherAssignmentDao assignmentDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.attendanceDao = Objects.requireNonNull(attendanceDao, "attendanceDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.registrationDao = Objects.requireNonNull(registrationDao, "registrationDao");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Monday and Sunday of the ISO week containing {@code date}. */
    public record Week(LocalDate monday, LocalDate sunday) {

        public static Week containing(LocalDate date) {
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new Week(monday, monday.plusDays(6));
        }
    }

    /**
     * Records the attendance of one student. An existing record for the same
     * day and subject is only changed when {@code allowCorrection} is true.
     *
     * @return {@code true} when an existing record was corrected
     */
    public boolean record(int studentId, int subjectId, LocalDate date, AttendanceStatus status, String notes,
                          int teacherId, boolean allowCorrection) {
        validate(date, status, notes);
        return transactions.inTransaction(connection -> {
            checkContext(connection, studentId, subjectId, date, teacherId);
            return save(connection, new Attendance(null, studentId, subjectId, date, status, notes, teacherId, null),
                    allowCorrection);
        });
    }

    /**
     * Records the attendance of a whole group in one transaction: either every
     * mark is saved or none is.
     *
     * @param statuses status per student id
     * @return number of corrected records
     */
    public int recordForGroup(int subjectId, LocalDate date, int teacherId, Map<Integer, AttendanceStatus> statuses,
                              boolean allowCorrection) {
        if (statuses.isEmpty()) {
            throw new ValidationException("No hay estudiantes para registrar.");
        }
        statuses.values().forEach(status -> validate(date, status, null));
        return transactions.inTransaction(connection -> {
            int corrected = 0;
            for (Map.Entry<Integer, AttendanceStatus> entry : statuses.entrySet()) {
                checkContext(connection, entry.getKey(), subjectId, date, teacherId);
                if (save(connection, new Attendance(null, entry.getKey(), subjectId, date, entry.getValue(), null,
                        teacherId, null), allowCorrection)) {
                    corrected++;
                }
            }
            return corrected;
        });
    }

    public List<Attendance> history(int studentId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ValidationException("La fecha final debe ser igual o posterior a la inicial.");
        }
        return transactions.read(connection -> {
            ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            return attendanceDao.findByStudent(connection, studentId, from, to);
        });
    }

    /** Students with more than {@value #MAX_WEEKLY_ABSENCES} absences in the ISO week of {@code anyDayOfWeek}. */
    public List<AbsenceReportRow> weeklyAbsenceReport(LocalDate anyDayOfWeek) {
        Week week = Week.containing(Objects.requireNonNull(anyDayOfWeek, "anyDayOfWeek"));
        return transactions.read(connection -> attendanceDao.findStudentsOverAbsenceLimit(connection, week.monday(),
                week.sunday(), MAX_WEEKLY_ABSENCES));
    }

    /** Absence counts per student between two dates (inclusive). */
    public List<AbsenceReportRow> absenceSummary(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ValidationException("La fecha final debe ser igual o posterior a la inicial.");
        }
        return transactions.read(connection -> attendanceDao.summarizeByStudent(connection, from, to));
    }

    private void validate(LocalDate date, AttendanceStatus status, String notes) {
        if (status == null) {
            throw new ValidationException("El estado de asistencia es obligatorio.");
        }
        ServiceChecks.notInFuture(date, LocalDate.now(clock), "fecha de asistencia");
        ServiceChecks.optionalText(notes, "observaciones", MAX_NOTES_LENGTH);
    }

    private void checkContext(Connection connection, int studentId, int subjectId, LocalDate date, int teacherId)
            throws SQLException {
        ServiceChecks.require(studentDao.findById(connection, studentId),
                "No existe un estudiante con id " + studentId + ".");
        Subject subject = ServiceChecks.require(subjectDao.findById(connection, subjectId),
                "No existe una materia con id " + subjectId + ".");
        int year = date.getYear();
        if (!registrationDao.hasActiveRegistration(connection, studentId, subjectId, year)) {
            throw new BusinessRuleException("El estudiante con id " + studentId + " no está inscrito en "
                    + subject.name() + " en " + year + ".");
        }
        if (!assignmentDao.isAssigned(connection, teacherId, subjectId, year)) {
            throw new BusinessRuleException("El profesor con id " + teacherId + " no tiene asignada la materia "
                    + subject.name() + " en " + year + ".");
        }
    }

    private boolean save(Connection connection, Attendance attendance, boolean allowCorrection) throws SQLException {
        Optional<Attendance> existing = attendanceDao.find(connection, attendance.studentId(),
                attendance.subjectId(), attendance.date());
        if (existing.isEmpty()) {
            attendanceDao.insert(connection, attendance);
            return false;
        }
        if (!allowCorrection) {
            throw new ConfirmationRequiredException("Ya existe asistencia registrada para el estudiante con id "
                    + attendance.studentId() + " el " + attendance.date() + " ("
                    + existing.get().status().dbValue() + "). Confirme la corrección para reemplazarla.");
        }
        attendanceDao.updateStatus(connection, existing.get().id(), attendance.status(), attendance.notes(),
                attendance.recordedBy());
        return true;
    }
}
