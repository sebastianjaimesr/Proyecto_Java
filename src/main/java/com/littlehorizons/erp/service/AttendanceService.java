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
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.report.AbsenceReportRow;
import com.littlehorizons.erp.util.FileNames;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.TextFileReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    /** Exercise 14: weekly report blocks accumulate here. */
    public static final Path ABSENCE_HISTORY = FileSupport.EXPORTS.resolve("ausencias_historico.txt");
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

    // ------------------------------------------------------ text file exercises

    /** Exercise 14: the weekly report and whether its block was added to the history file. */
    public record WeeklyHistory(List<AbsenceReportRow> rows, Path file, boolean added) {
    }

    /**
     * Exercise 14: runs the weekly report and appends its block to
     * {@link #ABSENCE_HISTORY}. The file is read first so the same week is
     * never written twice.
     */
    public WeeklyHistory weeklyReportWithHistory(LocalDate anyDayOfWeek) {
        Week week = Week.containing(Objects.requireNonNull(anyDayOfWeek, "anyDayOfWeek"));
        List<AbsenceReportRow> rows = weeklyAbsenceReport(anyDayOfWeek);
        String header = "=== Semana del " + week.monday() + " al " + week.sunday();
        boolean alreadySaved = Files.exists(ABSENCE_HISTORY)
                && FileSupport.readAllLines(ABSENCE_HISTORY, AttendanceService.class).stream()
                .anyMatch(line -> line.startsWith(header));
        if (alreadySaved) {
            return new WeeklyHistory(rows, ABSENCE_HISTORY.toAbsolutePath(), false);
        }
        List<String> block = new ArrayList<>();
        block.add(header + " (consultado el " + Formats.dateTime(LocalDateTime.now(clock).withNano(0)) + ") ===");
        if (rows.isEmpty()) {
            block.add("Ningún estudiante superó " + MAX_WEEKLY_ABSENCES + " ausencias.");
        }
        rows.forEach(row -> block.add(String.join(";", row.documentNumber(), row.studentName(),
                Formats.text(row.gradeName()), row.absences() + " ausencias")));
        block.add("");
        Path file = FileSupport.append(ABSENCE_HISTORY, block, AttendanceService.class);
        return new WeeklyHistory(rows, file, true);
    }

    /**
     * Exercise 17 (a): blank attendance sheet of a group, one line per student
     * with an empty field for the letter.
     */
    public Path exportAttendanceSheet(int subjectId, String section, LocalDate date) {
        String normalized = ServiceChecks.normalizeSection(section);
        return transactions.read(connection -> {
            Subject subject = ServiceChecks.require(subjectDao.findById(connection, subjectId),
                    "No existe una materia con id " + subjectId + ".");
            List<Student> students = registrationDao.findStudentsBySubject(connection, subjectId, date.getYear(),
                    normalized);
            if (students.isEmpty()) {
                throw new BusinessRuleException("No hay estudiantes inscritos en " + subject.name() + " en "
                        + date.getYear() + (normalized == null ? "" : " sección " + normalized) + ".");
            }
            List<String> lines = new ArrayList<>();
            lines.add("# Planilla de asistencia - " + subject.name() + " (" + subject.gradeName() + ")"
                    + (normalized == null ? "" : " - sección " + normalized) + " - " + date);
            lines.add("# Escriba después del último ';': P (Presente), A (Ausente), T (Tardanza) o E (Excusa médica)");
            lines.add("# documento;estudiante;asistencia");
            students.forEach(student -> lines.add(student.documentNumber() + ";" + student.name().sortableName() + ";"));
            Path file = FileSupport.EXPORTS.resolve("planillas").resolve("planilla_" + FileNames.safe(subject.code())
                    + (normalized == null ? "" : "_" + FileNames.safe(normalized)) + "_" + date + ".txt");
            return FileSupport.write(file, lines, AttendanceService.class);
        });
    }

    /** Exercise 17 (b): what was saved from a sheet and what was reported. */
    public record SheetResult(int recorded, int corrected, List<String> warnings) {
    }

    /**
     * Exercise 17 (b): reads a filled sheet and records the group with
     * {@link #recordForGroup}. Bad lines are reported and skipped; students
     * of the group missing from the sheet are reported too.
     */
    public SheetResult recordFromSheet(Path file, int subjectId, String section, LocalDate date, int teacherId,
                                       boolean allowCorrection) {
        String normalized = ServiceChecks.normalizeSection(section);
        List<TextFileReader.Line> lines = FileSupport.readDataLines(file, AttendanceService.class);
        List<Student> group = transactions.read(connection ->
                registrationDao.findStudentsBySubject(connection, subjectId, date.getYear(), normalized));
        Map<String, Student> byDocument = new LinkedHashMap<>();
        group.forEach(student -> byDocument.put(student.documentNumber(), student));

        Map<Integer, AttendanceStatus> statuses = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (TextFileReader.Line line : lines) {
            String[] f = line.fields();
            Student student = byDocument.get(f[0]);
            AttendanceStatus status = f.length >= 3 ? AttendanceStatus.fromLetter(f[2]) : null;
            if (student == null) {
                warnings.add(FileSupport.lineError(line.number(), "el documento " + f[0]
                        + " no pertenece al grupo de esta materia."));
            } else if (status == null) {
                warnings.add(FileSupport.lineError(line.number(), "falta la letra o no es P, A, T ni E ("
                        + student.name().sortableName() + ")."));
            } else if (statuses.putIfAbsent(student.id(), status) != null) {
                warnings.add(FileSupport.lineError(line.number(), "el estudiante está repetido en la planilla."));
            }
        }
        group.stream().filter(student -> !statuses.containsKey(student.id()))
                .filter(student -> lines.stream().noneMatch(line -> line.fields()[0].equals(student.documentNumber())))
                .forEach(student -> warnings.add("No aparece en la planilla: " + student.name().sortableName()));
        if (statuses.isEmpty()) {
            throw new ValidationException("La planilla no tiene ninguna asistencia válida."
                    + (warnings.isEmpty() ? "" : "\n" + String.join("\n", warnings)));
        }
        int corrected = recordForGroup(subjectId, date, teacherId, statuses, allowCorrection);
        return new SheetResult(statuses.size(), corrected, warnings);
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
