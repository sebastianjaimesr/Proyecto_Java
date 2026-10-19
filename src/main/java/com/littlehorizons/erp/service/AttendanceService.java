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
 * Asistencia diaria por materia y reporte semanal de inasistencias.
 *
 * <p>Regla aprobada (D-06): la semana de análisis es la semana ISO, de lunes a
 * domingo, que contiene la fecha elegida. Solo "Ausente" cuenta como
 * inasistencia; "Tardanza" y "Excusa Médica" se reportan aparte. Un estudiante
 * aparece cuando sus ausencias de la semana, sumando todas las materias, son más
 * de tres.</p>
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

    /** Lunes y domingo de la semana ISO que contiene {@code date}. */
    public record Week(LocalDate monday, LocalDate sunday) {

        public static Week containing(LocalDate date) {
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new Week(monday, monday.plusDays(6));
        }
    }

    /**
     * Registra la asistencia de un estudiante. Un registro existente del mismo
     * día y materia solo se cambia cuando {@code allowCorrection} es true.
     *
     * @return {@code true} cuando se corrigió un registro existente
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
     * Registra la asistencia de todo un grupo en una transacción: se guardan todas
     * las marcas o ninguna.
     *
     * @param statuses estado por id de estudiante
     * @return cantidad de registros corregidos
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

    /** Historial de asistencia de un estudiante entre dos fechas; valida que el rango tenga sentido. */
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

    /** Estudiantes con más de {@value #MAX_WEEKLY_ABSENCES} ausencias en la semana ISO de {@code anyDayOfWeek}. */
    public List<AbsenceReportRow> weeklyAbsenceReport(LocalDate anyDayOfWeek) {
        Week week = Week.containing(Objects.requireNonNull(anyDayOfWeek, "anyDayOfWeek"));
        return transactions.read(connection -> attendanceDao.findStudentsOverAbsenceLimit(connection, week.monday(),
                week.sunday(), MAX_WEEKLY_ABSENCES));
    }

    /** Conteo de ausencias por estudiante entre dos fechas (inclusive). */
    public List<AbsenceReportRow> absenceSummary(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ValidationException("La fecha final debe ser igual o posterior a la inicial.");
        }
        return transactions.read(connection -> attendanceDao.summarizeByStudent(connection, from, to));
    }

    /**
     * Valida los datos de una marca de asistencia (estado obligatorio, fecha no futura, longitud de las notas).
     */
    private void validate(LocalDate date, AttendanceStatus status, String notes) {
        if (status == null) {
            throw new ValidationException("El estado de asistencia es obligatorio.");
        }
        ServiceChecks.notInFuture(date, LocalDate.now(clock), "fecha de asistencia");
        ServiceChecks.optionalText(notes, "observaciones", MAX_NOTES_LENGTH);
    }

    /**
     * Verifica que existan el estudiante, la materia y el profesor, y que el profesor esté asignado a la materia.
     */
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

    /**
     * Guarda la marca de asistencia: inserta si no existe; si existe, solo la corrige con permiso.
     * Devuelve true si corrigió un registro existente.
     */
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
