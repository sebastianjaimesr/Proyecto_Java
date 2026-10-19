package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeRecordDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectRegistrationDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfirmationRequiredException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.AcademicPeriod;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.academic.RegistrationStatus;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.SubjectRegistration;
import com.littlehorizons.erp.model.report.AcademicHistory;
import com.littlehorizons.erp.model.report.CourseAverageReport;
import com.littlehorizons.erp.model.report.StudentAverageReport;
import com.littlehorizons.erp.model.report.SubjectAverage;
import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Inscripciones a materias, notas, promedios e historial académico.
 */
public class AcademicService {

    private static final int MAX_NOTES_LENGTH = 1000;

    private final TransactionManager transactions;
    private final StudentDao studentDao;
    private final EnrollmentDao enrollmentDao;
    private final SubjectDao subjectDao;
    private final SubjectRegistrationDao registrationDao;
    private final GradeRecordDao gradeRecordDao;
    private final TeacherAssignmentDao assignmentDao;
    private final AverageCalculator calculator;
    private final Clock clock;

    public AcademicService(TransactionManager transactions, StudentDao studentDao, EnrollmentDao enrollmentDao,
                           SubjectDao subjectDao, SubjectRegistrationDao registrationDao,
                           GradeRecordDao gradeRecordDao, TeacherAssignmentDao assignmentDao,
                           AverageCalculator calculator, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.enrollmentDao = Objects.requireNonNull(enrollmentDao, "enrollmentDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.registrationDao = Objects.requireNonNull(registrationDao, "registrationDao");
        this.gradeRecordDao = Objects.requireNonNull(gradeRecordDao, "gradeRecordDao");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // ---------------------------------------------------------- inscripciones

    /**
     * Inscribe a un estudiante en una materia para un período. El estudiante necesita una
     * matrícula activa en ese año y la materia debe pertenecer al
     * grado en que está matriculado.
     */
    public SubjectRegistration register(int studentId, int subjectId, int academicYear, AcademicPeriod period,
                                        String rawSection, LocalDate date) {
        String section = ServiceChecks.normalizeSection(rawSection);
        validateRegistrationInput(academicYear, period, date);
        return transactions.inTransaction(connection -> {
            Enrollment enrollment = requireActiveEnrollment(connection, studentId, academicYear);
            Subject subject = requireSubject(connection, subjectId);
            checkSubjectForEnrollment(subject, enrollment);
            if (registrationDao.find(connection, studentId, subjectId, academicYear, period).isPresent()) {
                throw new BusinessRuleException("El estudiante ya está inscrito en " + subject.name()
                        + " para el período " + period.dbValue() + " de " + academicYear + ".");
            }
            return insertRegistration(connection, studentId, subject, academicYear, period, section, date);
        });
    }

    /**
     * Inscribe al estudiante en todas las materias activas del grado matriculado para
     * un período, en una sola transacción. Las materias en las que ya estaba inscrito
     * se omiten.
     *
     * @return las inscripciones nuevas
     */
    public List<SubjectRegistration> registerAllSubjects(int studentId, int academicYear, AcademicPeriod period,
                                                         String rawSection, LocalDate date) {
        String section = ServiceChecks.normalizeSection(rawSection);
        validateRegistrationInput(academicYear, period, date);
        return transactions.inTransaction(connection -> {
            Enrollment enrollment = requireActiveEnrollment(connection, studentId, academicYear);
            List<Subject> subjects = subjectDao.findByGrade(connection, enrollment.gradeId(), true);
            if (subjects.isEmpty()) {
                throw new BusinessRuleException("El grado " + enrollment.gradeName() + " no tiene materias activas.");
            }
            List<SubjectRegistration> created = new ArrayList<>();
            for (Subject subject : subjects) {
                if (registrationDao.find(connection, studentId, subject.id(), academicYear, period).isEmpty()) {
                    created.add(insertRegistration(connection, studentId, subject, academicYear, period, section, date));
                }
            }
            return created;
        });
    }

    /** Marca una inscripción como retirada; las notas ya registradas se conservan. */
    public void withdrawRegistration(int studentId, int subjectId, int academicYear, AcademicPeriod period) {
        transactions.inTransactionVoid(connection -> {
            SubjectRegistration registration = ServiceChecks.require(
                    registrationDao.find(connection, studentId, subjectId, academicYear, period),
                    "No existe esa inscripción.");
            if (registration.status() == RegistrationStatus.WITHDRAWN) {
                throw new BusinessRuleException("La inscripción ya estaba retirada.");
            }
            registrationDao.updateStatus(connection, registration.id(), RegistrationStatus.WITHDRAWN);
        });
    }

    public List<SubjectRegistration> findRegistrations(int studentId, int academicYear) {
        return transactions.read(connection -> registrationDao.findByStudent(connection, studentId, academicYear));
    }

    public List<Student> findStudentsInSubject(int subjectId, int academicYear, String section) {
        String normalized = ServiceChecks.normalizeSection(section);
        return transactions.read(connection ->
                registrationDao.findStudentsBySubject(connection, subjectId, academicYear, normalized));
    }

    // ------------------------------------------------------------------ notas

    /** Resultado de registrar una nota. */
    public record GradeResult(GradeRecord gradeRecord, boolean corrected, BigDecimal previousValue) {
    }

    /**
     * Registra una nota para los períodos I a IV. El estudiante debe estar inscrito en
     * la materia para ese período y el profesor debe estar asignado a la
     * materia en ese año. Una nota existente solo se reemplaza cuando
     * {@code allowCorrection} es true (la vista le pregunta antes al usuario).
     */
    public GradeResult recordGrade(int studentId, int subjectId, int teacherId, int academicYear,
                                   GradingPeriod period, BigDecimal value, String notes, boolean allowCorrection) {
        if (period == null || !period.isEditable()) {
            throw new ValidationException("La nota Final se calcula automáticamente; registre los períodos I a IV.");
        }
        BigDecimal grade = InputValidator.parseGrade(value == null ? null : value.toPlainString());
        ServiceChecks.optionalText(notes, "observaciones", MAX_NOTES_LENGTH);
        LocalDate today = LocalDate.now(clock);
        ServiceChecks.academicYear(academicYear, today);
        return transactions.inTransaction(connection -> {
            Subject subject = requireSubject(connection, subjectId);
            AcademicPeriod registrationPeriod = AcademicPeriod.valueOf(period.name());
            SubjectRegistration registration = ServiceChecks.require(
                    registrationDao.find(connection, studentId, subjectId, academicYear, registrationPeriod),
                    "El estudiante no está inscrito en " + subject.name() + " para el período "
                            + period.dbValue() + " de " + academicYear + ".");
            if (registration.status() == RegistrationStatus.WITHDRAWN) {
                throw new BusinessRuleException("La inscripción del estudiante en esa materia y período está retirada.");
            }
            requireAssignedTeacher(connection, teacherId, subject, academicYear);
            Optional<GradeRecord> existing = gradeRecordDao.find(connection, studentId, subjectId, academicYear, period);
            if (existing.isPresent()) {
                if (!allowCorrection) {
                    throw new ConfirmationRequiredException("Ya existe una nota de " + existing.get().value()
                            + " para ese período. Confirme la corrección para reemplazarla.");
                }
                gradeRecordDao.updateValue(connection, existing.get().id(), teacherId, grade, notes, today);
                return new GradeResult(gradeRecordDao.find(connection, studentId, subjectId, academicYear, period)
                        .orElseThrow(), true, existing.get().value());
            }
            gradeRecordDao.insert(connection, new GradeRecord(null, studentId, subjectId, teacherId, academicYear,
                    period, grade, notes, today, null, null, null));
            return new GradeResult(gradeRecordDao.find(connection, studentId, subjectId, academicYear, period)
                    .orElseThrow(), false, null);
        });
    }

    /**
     * Calcula la nota Final como la media simple de los períodos I a IV que tengan
     * nota y la guarda (reemplazando una Final anterior). Devuelve el registro
     * guardado; si hay menos de cuatro períodos, avisar es tarea de quien llama.
     */
    public GradeResult computeFinalGrade(int studentId, int subjectId, int academicYear, int teacherId) {
        LocalDate today = LocalDate.now(clock);
        return transactions.inTransaction(connection -> {
            Subject subject = requireSubject(connection, subjectId);
            requireAssignedTeacher(connection, teacherId, subject, academicYear);
            List<GradeRecord> records = gradeRecordDao.findByStudent(connection, studentId, academicYear).stream()
                    .filter(gradeRecord -> gradeRecord.subjectId() == subjectId)
                    .collect(Collectors.toList());
            BigDecimal mean = calculator.periodMean(records);
            if (mean == null) {
                throw new BusinessRuleException("El estudiante no tiene notas de los períodos I a IV en "
                        + subject.name() + ".");
            }
            Optional<GradeRecord> existing = records.stream()
                    .filter(gradeRecord -> gradeRecord.period() == GradingPeriod.FINAL).findFirst();
            String note = "Promedio automático de " + records.stream()
                    .filter(gradeRecord -> gradeRecord.period().isEditable()).count() + " período(s)";
            if (existing.isPresent()) {
                gradeRecordDao.updateValue(connection, existing.get().id(), teacherId, mean, note, today);
            } else {
                gradeRecordDao.insert(connection, new GradeRecord(null, studentId, subjectId, teacherId,
                        academicYear, GradingPeriod.FINAL, mean, note, today, null, null, null));
            }
            return new GradeResult(gradeRecordDao.find(connection, studentId, subjectId, academicYear,
                    GradingPeriod.FINAL).orElseThrow(), existing.isPresent(),
                    existing.map(GradeRecord::value).orElse(null));
        });
    }

    public List<GradeRecord> findGradesByStudent(int studentId, Integer academicYear) {
        return transactions.read(connection -> gradeRecordDao.findByStudent(connection, studentId, academicYear));
    }

    /** Notas de una materia, opcionalmente filtradas por sección y período. */
    public List<GradeRecord> findGradesBySubject(int subjectId, int academicYear, String section,
                                                 GradingPeriod period) {
        String normalized = ServiceChecks.normalizeSection(section);
        return transactions.read(connection -> gradeRecordDao.findBySubject(connection, subjectId, academicYear,
                        normalized)).stream()
                .filter(gradeRecord -> period == null || gradeRecord.period() == period)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- promedios

    /** Promedios por materia y promedio general de un estudiante en un año. */
    public StudentAverageReport studentAverages(int studentId, int academicYear) {
        return transactions.read(connection -> {
            Student student = requireStudent(connection, studentId);
            List<GradeRecord> records = gradeRecordDao.findByStudent(connection, studentId, academicYear);
            List<SubjectAverage> subjects = new ArrayList<>(calculator.subjectAverages(records));
            // También se listan las materias inscritas sin notas, para que el reporte las muestre explícitamente.
            Set<Integer> withGrades = subjects.stream().map(SubjectAverage::subjectId).collect(Collectors.toSet());
            registrationDao.findByStudent(connection, studentId, academicYear).stream()
                    .filter(registration -> registration.status() != RegistrationStatus.WITHDRAWN)
                    .filter(registration -> !withGrades.contains(registration.subjectId()))
                    .collect(Collectors.toMap(SubjectRegistration::subjectId, SubjectRegistration::subjectName,
                            (first, second) -> first))
                    .forEach((subjectId, subjectName) ->
                            subjects.add(new SubjectAverage(subjectId, subjectName, Map.of(), null, false)));
            subjects.sort(Comparator.comparing(SubjectAverage::subjectName));
            String gradeName = enrollmentDao.findByStudentAndYear(connection, studentId, academicYear)
                    .map(Enrollment::gradeName).orElse(null);
            long withoutGrades = subjects.stream().filter(subject -> !subject.hasGrades()).count();
            return new StudentAverageReport(studentId, student.fullName(), academicYear, gradeName, subjects,
                    calculator.generalAverage(subjects), withoutGrades);
        });
    }

    /** Promedio de una materia para todo el curso (grado de la materia, sección opcional). */
    public CourseAverageReport courseAverage(int subjectId, int academicYear, String section) {
        String normalized = ServiceChecks.normalizeSection(section);
        return transactions.read(connection -> {
            Subject subject = requireSubject(connection, subjectId);
            List<GradeRecord> records = gradeRecordDao.findBySubject(connection, subjectId, academicYear, normalized);
            List<CourseAverageReport.StudentGrade> students = calculator.studentGrades(records);
            return new CourseAverageReport(subjectId, subject.name(), subject.gradeName(), academicYear, normalized,
                    students, calculator.courseAverage(students), calculator.highest(students).orElse(null),
                    calculator.lowest(students).orElse(null));
        });
    }

    /** Todos los años del estudiante, desde su primera matrícula o nota hasta hoy. */
    public AcademicHistory history(int studentId) {
        return transactions.read(connection -> {
            Student student = requireStudent(connection, studentId);
            Map<Integer, List<GradeRecord>> gradesByYear =
                    AverageCalculator.byYear(gradeRecordDao.findByStudent(connection, studentId, null));
            Map<Integer, Enrollment> enrollments = enrollmentDao.findByStudent(connection, studentId).stream()
                    .collect(Collectors.toMap(Enrollment::academicYear, enrollment -> enrollment));
            Set<Integer> years = new TreeSet<>(gradesByYear.keySet());
            years.addAll(enrollments.keySet());
            List<AcademicHistory.YearSummary> summaries = years.stream().map(year -> {
                List<SubjectAverage> subjects = calculator.subjectAverages(gradesByYear.getOrDefault(year, List.of()));
                Enrollment enrollment = enrollments.get(year);
                return new AcademicHistory.YearSummary(year,
                        enrollment == null ? null : enrollment.gradeName(),
                        enrollment == null ? null : enrollment.status().dbValue(),
                        subjects, calculator.generalAverage(subjects));
            }).collect(Collectors.toList());
            return new AcademicHistory(studentId, student.fullName(), summaries);
        });
    }

    // ----------------------------------------------------------------- auxiliares

    private void validateRegistrationInput(int academicYear, AcademicPeriod period, LocalDate date) {
        if (period == null) {
            throw new ValidationException("El período es obligatorio.");
        }
        LocalDate today = LocalDate.now(clock);
        ServiceChecks.academicYear(academicYear, today);
        ServiceChecks.notInFuture(date, today, "fecha de inscripción");
    }

    private SubjectRegistration insertRegistration(Connection connection, int studentId, Subject subject,
                                                   int academicYear, AcademicPeriod period, String section,
                                                   LocalDate date) throws SQLException {
        SubjectRegistration draft = new SubjectRegistration(null, studentId, subject.id(), subject.name(),
                academicYear, period, section, date, RegistrationStatus.ACTIVE);
        int id = registrationDao.insert(connection, draft);
        return new SubjectRegistration(id, studentId, subject.id(), subject.name(), academicYear, period, section,
                date, RegistrationStatus.ACTIVE);
    }

    private Enrollment requireActiveEnrollment(Connection connection, int studentId, int academicYear)
            throws SQLException {
        requireStudent(connection, studentId);
        Enrollment enrollment = ServiceChecks.require(
                enrollmentDao.findByStudentAndYear(connection, studentId, academicYear),
                "El estudiante no tiene matrícula para el año " + academicYear + ".");
        if (enrollment.status() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("La matrícula del año " + academicYear + " no está activa ("
                    + enrollment.status().dbValue() + ").");
        }
        return enrollment;
    }

    private static void checkSubjectForEnrollment(Subject subject, Enrollment enrollment) {
        if (!subject.active()) {
            throw new BusinessRuleException("La materia " + subject.name() + " está inactiva.");
        }
        if (subject.gradeId() != enrollment.gradeId()) {
            throw new BusinessRuleException("La materia " + subject.name() + " pertenece a " + subject.gradeName()
                    + " y el estudiante está matriculado en " + enrollment.gradeName() + ".");
        }
    }

    private void requireAssignedTeacher(Connection connection, int teacherId, Subject subject, int academicYear)
            throws SQLException {
        if (!assignmentDao.isAssigned(connection, teacherId, subject.id(), academicYear)) {
            throw new BusinessRuleException("El profesor con id " + teacherId + " no tiene asignada la materia "
                    + subject.name() + " en " + academicYear + ".");
        }
    }

    private Student requireStudent(Connection connection, int studentId) throws SQLException {
        return ServiceChecks.require(studentDao.findById(connection, studentId),
                "No existe un estudiante con id " + studentId + ".");
    }

    private Subject requireSubject(Connection connection, int subjectId) throws SQLException {
        return ServiceChecks.require(subjectDao.findById(connection, subjectId),
                "No existe una materia con id " + subjectId + ".");
    }
}
