package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcAttendanceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcCatalogDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGradeRecordDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectRegistrationDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherAssignmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfirmationRequiredException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.AcademicPeriod;
import com.littlehorizons.erp.model.academic.AttendanceStatus;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.SubjectRegistration;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.academic.WeekDay;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.model.report.AbsenceReportRow;
import com.littlehorizons.erp.model.report.CourseAverageReport;
import com.littlehorizons.erp.model.report.StudentAverageReport;
import com.littlehorizons.erp.support.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Academic module against a real MySQL test database created from
 * docs/database.sql. Each test builds its own data with unique documents.
 */
class AcademicModuleIntegrationTest {

    private static final int YEAR = 2026;
    private static final int GRADE_FIFTH = 6;   // id of "Quinto" in the official seed (Preescolar = 1)
    private static final int GRADE_SIXTH = 7;   // id of "Sexto"
    private static final AtomicInteger SEQUENCE = new AtomicInteger(100);

    private static TestDatabase database;
    private static TransactionManager transactions;
    private static StudentService students;
    private static GuardianService guardians;
    private static EnrollmentService enrollments;
    private static SubjectService subjects;
    private static TeacherService teachers;
    private static AcademicService academic;
    private static AttendanceService attendance;

    @BeforeAll
    static void createDatabase() {
        database = TestDatabase.recreate();
        transactions = database.transactionManager();
        JdbcStudentDao studentDao = new JdbcStudentDao();
        JdbcGuardianDao guardianDao = new JdbcGuardianDao();
        JdbcEnrollmentDao enrollmentDao = new JdbcEnrollmentDao();
        JdbcCatalogDao catalogDao = new JdbcCatalogDao();
        JdbcSubjectDao subjectDao = new JdbcSubjectDao();
        JdbcTeacherAssignmentDao assignmentDao = new JdbcTeacherAssignmentDao();
        JdbcSubjectRegistrationDao registrationDao = new JdbcSubjectRegistrationDao();
        students = new StudentService(transactions, studentDao, guardianDao, enrollmentDao, TestDatabase.CLOCK);
        guardians = new GuardianService(transactions, guardianDao, studentDao);
        enrollments = new EnrollmentService(transactions, enrollmentDao, studentDao, catalogDao, TestDatabase.CLOCK);
        subjects = new SubjectService(transactions, subjectDao, catalogDao);
        teachers = new TeacherService(transactions, new JdbcTeacherDao(), subjectDao, assignmentDao, TestDatabase.CLOCK);
        academic = new AcademicService(transactions, studentDao, enrollmentDao, subjectDao, registrationDao,
                new JdbcGradeRecordDao(), assignmentDao, new AverageCalculator(new ArithmeticMeanStrategy()),
                TestDatabase.CLOCK);
        attendance = new AttendanceService(transactions, new JdbcAttendanceDao(), studentDao, subjectDao,
                registrationDao, assignmentDao, TestDatabase.CLOCK);
    }

    // ---------------------------------------------------------------- data

    private static Student newStudent() {
        int n = SEQUENCE.incrementAndGet();
        return students.register(new Student(null, StudentDocumentType.TI, "TI" + (1_000_000 + n),
                new PersonName("Sofía", null, "Prueba" + n, "Gómez"), LocalDate.of(2015, 3, 10), Gender.FEMALE,
                "Calle 10 # 5-20, Bogotá", "3001234567", null, "O+", null, null,
                LocalDate.of(2021, 1, 20), StudentStatus.ACTIVE));
    }

    private static Guardian guardianDraft() {
        int n = SEQUENCE.incrementAndGet();
        return new Guardian(null, PersonDocumentType.CC, "CC" + (52_000_000 + n),
                new PersonName("Carlos", null, "Acudiente" + n, null), "Padre", "3109876543", null,
                "carlos" + n + "@correo.com", "Carrera 7 # 80-10", "Ingeniero", true);
    }

    private static Teacher newTeacher() {
        int n = SEQUENCE.incrementAndGet();
        return teachers.register(new Teacher(null, PersonDocumentType.CC, "CC" + (79_000_000 + n),
                new PersonName("Laura", null, "Docente" + n, null), "docente" + n + "@littlehorizons.edu.co",
                "3205551234", "Matemáticas", LocalDate.of(2018, 2, 1), TeacherStatus.ACTIVE));
    }

    private static Subject newSubject(int gradeId) {
        int n = SEQUENCE.incrementAndGet();
        return subjects.register(new Subject(null, "MAT-" + n, "Matemáticas " + n, null, gradeId, null, 5, true));
    }

    // ------------------------------------------------------------- students

    @Test
    @DisplayName("Duplicate student documents are rejected")
    void duplicateStudentDocumentIsRejected() {
        Student student = newStudent();
        Student copy = new Student(null, StudentDocumentType.TI, student.documentNumber(),
                new PersonName("Otro", null, "Nombre", null), LocalDate.of(2014, 5, 5), Gender.MALE, null, null,
                null, null, null, null, LocalDate.of(2022, 1, 15), StudentStatus.ACTIVE);
        assertThrows(BusinessRuleException.class, () -> students.register(copy));
    }

    @Test
    void invalidStudentDataIsRejectedBeforeTheDatabase() {
        Student futureBirth = new Student(null, StudentDocumentType.TI, "TI7777777",
                new PersonName("Ana", null, "Ruiz", null), TestDatabase.TODAY.plusDays(1), Gender.FEMALE, null,
                null, null, null, null, null, TestDatabase.TODAY, StudentStatus.ACTIVE);
        assertThrows(ValidationException.class, () -> students.register(futureBirth));
    }

    @Test
    void guardianIsLinkedAndLastGuardianCannotBeRemoved() {
        Student student = newStudent();
        Guardian guardian = guardians.registerForStudent(guardianDraft(), student.id(), true, true);

        assertEquals(1, guardians.findGuardiansOfStudent(student.id()).size());
        assertThrows(BusinessRuleException.class, () -> guardians.unlink(student.id(), guardian.id()));
    }

    @Test
    void siblingsShareAGuardian() {
        Student first = newStudent();
        Student second = newStudent();
        Guardian guardian = guardians.registerForStudent(guardianDraft(), first.id(), true, true);
        guardians.link(second.id(), guardian.id(), true, true);

        assertEquals(List.of(second.id()), students.getProfile(first.id()).siblings().stream()
                .map(Student::id).toList());
        assertEquals(2, guardians.findStudentsOfGuardian(guardian.id()).size());
    }

    @Test
    void onlyOneEnrollmentPerYear() {
        Student student = newStudent();
        enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);
        assertThrows(BusinessRuleException.class,
                () -> enrollments.enroll(student.id(), GRADE_SIXTH, YEAR, LocalDate.of(2026, 1, 21), null));
    }

    @Test
    @DisplayName("Withdrawing a student also closes the active enrollment in one transaction")
    void withdrawalClosesEnrollment() {
        Student student = newStudent();
        Enrollment enrollment = enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);

        students.withdraw(student.id(), LocalDate.of(2026, 6, 30));

        assertEquals(StudentStatus.WITHDRAWN, students.findById(student.id()).status());
        Enrollment closed = enrollments.history(student.id()).get(0);
        assertEquals(enrollment.id(), closed.id());
        assertEquals(EnrollmentStatus.WITHDRAWN, closed.status());
        assertEquals(LocalDate.of(2026, 6, 30), closed.withdrawalDate());
        assertThrows(BusinessRuleException.class,
                () -> enrollments.enroll(student.id(), GRADE_FIFTH, YEAR + 1, TestDatabase.TODAY, null));
    }

    @Test
    @DisplayName("A failed transaction leaves no partial data")
    void rollbackLeavesNoPartialData() {
        String document = "TI9990001";
        assertThrows(IllegalStateException.class, () -> transactions.inTransaction(connection -> {
            new JdbcStudentDao().insert(connection, new Student(null, StudentDocumentType.TI, document,
                    new PersonName("Temporal", null, "Rollback", null), LocalDate.of(2015, 1, 1), Gender.OTHER,
                    null, null, null, null, null, null, LocalDate.of(2022, 1, 1), StudentStatus.ACTIVE));
            throw new IllegalStateException("simulated failure after the insert");
        }));
        assertThrows(BusinessRuleException.class, () -> students.findByDocument(document));
    }

    @Test
    @DisplayName("Foreign keys are enforced by the database and reported as business errors")
    void referentialIntegrityIsEnforced() {
        assertThrows(BusinessRuleException.class, () -> transactions.inTransaction(connection ->
                new JdbcSubjectRegistrationDao().insert(connection, new SubjectRegistration(null, 999_999, 999_999,
                        null, YEAR, AcademicPeriod.I, null, TestDatabase.TODAY,
                        com.littlehorizons.erp.model.academic.RegistrationStatus.ACTIVE))));
    }

    // -------------------------------------------------------- registrations

    @Test
    void registrationRequiresEnrollmentAndMatchingGrade() {
        Student student = newStudent();
        Subject fifth = newSubject(GRADE_FIFTH);
        Subject sixth = newSubject(GRADE_SIXTH);

        assertThrows(BusinessRuleException.class,
                () -> academic.register(student.id(), fifth.id(), YEAR, AcademicPeriod.I, "A", TestDatabase.TODAY));

        enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);
        assertThrows(BusinessRuleException.class,
                () -> academic.register(student.id(), sixth.id(), YEAR, AcademicPeriod.I, "A", TestDatabase.TODAY));

        academic.register(student.id(), fifth.id(), YEAR, AcademicPeriod.I, "a", TestDatabase.TODAY);
        assertThrows(BusinessRuleException.class,
                () -> academic.register(student.id(), fifth.id(), YEAR, AcademicPeriod.I, "A", TestDatabase.TODAY));
        assertEquals("A", academic.findRegistrations(student.id(), YEAR).get(0).section());
    }

    @Test
    void registerAllSubjectsSkipsExistingOnes() {
        Student student = newStudent();
        enrollments.enroll(student.id(), GRADE_SIXTH, YEAR, LocalDate.of(2026, 1, 20), null);
        int activeSubjects = subjects.findByGrade(GRADE_SIXTH, true).size();
        Subject extra = newSubject(GRADE_SIXTH);
        academic.register(student.id(), extra.id(), YEAR, AcademicPeriod.II, null, TestDatabase.TODAY);

        List<SubjectRegistration> created = academic.registerAllSubjects(student.id(), YEAR, AcademicPeriod.II,
                null, TestDatabase.TODAY);

        assertEquals(activeSubjects, created.size());
    }

    // --------------------------------------------------------------- grades

    @Test
    void gradesAreValidatedCorrectedOnlyWithConfirmationAndFinalIsTheMean() {
        Student student = newStudent();
        Teacher teacher = newTeacher();
        Teacher other = newTeacher();
        Subject subject = newSubject(GRADE_FIFTH);
        enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);
        teachers.assign(teacher.id(), subject.id(), YEAR, "A");
        for (AcademicPeriod period : AcademicPeriod.values()) {
            academic.register(student.id(), subject.id(), YEAR, period, "A", TestDatabase.TODAY);
        }

        assertThrows(ValidationException.class, () -> academic.recordGrade(student.id(), subject.id(), teacher.id(),
                YEAR, GradingPeriod.I, new BigDecimal("5.5"), null, false));
        assertThrows(ValidationException.class, () -> academic.recordGrade(student.id(), subject.id(), teacher.id(),
                YEAR, GradingPeriod.FINAL, new BigDecimal("4.0"), null, false));
        assertThrows(BusinessRuleException.class, () -> academic.recordGrade(student.id(), subject.id(), other.id(),
                YEAR, GradingPeriod.I, new BigDecimal("4.0"), null, false));

        academic.recordGrade(student.id(), subject.id(), teacher.id(), YEAR, GradingPeriod.I, new BigDecimal("3.0"),
                null, false);
        assertThrows(ConfirmationRequiredException.class, () -> academic.recordGrade(student.id(), subject.id(),
                teacher.id(), YEAR, GradingPeriod.I, new BigDecimal("3.5"), null, false));
        AcademicService.GradeResult corrected = academic.recordGrade(student.id(), subject.id(), teacher.id(), YEAR,
                GradingPeriod.I, new BigDecimal("3.5"), "Corrección", true);
        assertTrue(corrected.corrected());
        assertEquals(new BigDecimal("3.0"), corrected.previousValue());

        academic.recordGrade(student.id(), subject.id(), teacher.id(), YEAR, GradingPeriod.II, new BigDecimal("4.0"),
                null, false);
        academic.recordGrade(student.id(), subject.id(), teacher.id(), YEAR, GradingPeriod.III, new BigDecimal("4.2"),
                null, false);
        // (3.5 + 4.0 + 4.2) / 3 = 3.9
        AcademicService.GradeResult finalGrade = academic.computeFinalGrade(student.id(), subject.id(), YEAR,
                teacher.id());
        assertEquals(new BigDecimal("3.9"), finalGrade.gradeRecord().value());
        assertEquals(GradingPeriod.FINAL, finalGrade.gradeRecord().period());
    }

    @Test
    void generalAverageExcludesSubjectsWithoutGrades() {
        Student student = newStudent();
        Teacher teacher = newTeacher();
        Subject graded = newSubject(GRADE_FIFTH);
        Subject ungraded = newSubject(GRADE_FIFTH);
        enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);
        teachers.assign(teacher.id(), graded.id(), YEAR, null);
        academic.register(student.id(), graded.id(), YEAR, AcademicPeriod.I, null, TestDatabase.TODAY);
        academic.register(student.id(), ungraded.id(), YEAR, AcademicPeriod.I, null, TestDatabase.TODAY);
        academic.recordGrade(student.id(), graded.id(), teacher.id(), YEAR, GradingPeriod.I, new BigDecimal("4.5"),
                null, false);

        StudentAverageReport report = academic.studentAverages(student.id(), YEAR);

        assertEquals(new BigDecimal("4.5"), report.generalAverage());
        assertEquals(1, report.subjectsWithoutGrades());
        assertEquals(2, report.subjects().size());
    }

    @Test
    void courseAverageUsesEveryStudentOfTheSubject() {
        Teacher teacher = newTeacher();
        Subject subject = newSubject(GRADE_SIXTH);
        teachers.assign(teacher.id(), subject.id(), YEAR, "B");
        String[] values = {"2.0", "3.0", "4.6"};
        for (String value : values) {
            Student student = newStudent();
            enrollments.enroll(student.id(), GRADE_SIXTH, YEAR, LocalDate.of(2026, 1, 20), null);
            academic.register(student.id(), subject.id(), YEAR, AcademicPeriod.I, "B", TestDatabase.TODAY);
            academic.recordGrade(student.id(), subject.id(), teacher.id(), YEAR, GradingPeriod.I,
                    new BigDecimal(value), null, false);
        }

        CourseAverageReport report = academic.courseAverage(subject.id(), YEAR, "b");

        assertEquals(3, report.students().size());
        assertEquals(new BigDecimal("3.2"), report.courseAverage());
        assertEquals(new BigDecimal("4.6"), report.highest());
        assertEquals(new BigDecimal("2.0"), report.lowest());
    }

    // ------------------------------------------------------------ schedules

    @Test
    void scheduleConflictsAreDetectedForTeacherAndGroup() {
        Teacher teacher = newTeacher();
        Teacher otherTeacher = newTeacher();
        Subject math = newSubject(GRADE_FIFTH);
        Subject science = newSubject(GRADE_FIFTH);
        TeacherAssignment first = teachers.assign(teacher.id(), math.id(), YEAR, "C");
        TeacherAssignment sameTeacher = teachers.assign(teacher.id(), science.id(), YEAR, "D");
        TeacherAssignment sameGroup = teachers.assign(otherTeacher.id(), science.id(), YEAR, "C");

        teachers.addSchedule(first.id(), WeekDay.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0), "Aula 101");

        assertThrows(BusinessRuleException.class, () -> teachers.addSchedule(sameTeacher.id(), WeekDay.MONDAY,
                LocalTime.of(7, 30), LocalTime.of(8, 30), "Aula 102"));
        assertThrows(BusinessRuleException.class, () -> teachers.addSchedule(sameGroup.id(), WeekDay.MONDAY,
                LocalTime.of(7, 45), LocalTime.of(9, 0), "Aula 101"));
        teachers.addSchedule(sameGroup.id(), WeekDay.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "Aula 101");
        assertThrows(ValidationException.class, () -> teachers.addSchedule(first.id(), WeekDay.TUESDAY,
                LocalTime.of(9, 0), LocalTime.of(8, 0), null));
        assertEquals(1, teachers.findTimetable(teacher.id(), YEAR).size());
    }

    @Test
    void duplicateAssignmentIsRejectedEvenWithoutSection() {
        Teacher teacher = newTeacher();
        Subject subject = newSubject(GRADE_FIFTH);
        teachers.assign(teacher.id(), subject.id(), YEAR, null);
        assertThrows(BusinessRuleException.class, () -> teachers.assign(teacher.id(), subject.id(), YEAR, " "));
    }

    // ----------------------------------------------------------- attendance

    @Test
    @DisplayName("Weekly report lists students with more than three 'Ausente' marks only")
    void weeklyAbsenceReportCountsOnlyUnexcusedAbsences() {
        Teacher teacher = newTeacher();
        Subject subject = newSubject(GRADE_FIFTH);
        teachers.assign(teacher.id(), subject.id(), YEAR, null);
        Student absent = newStudent();
        Student excused = newStudent();
        for (Student student : List.of(absent, excused)) {
            enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);
            academic.register(student.id(), subject.id(), YEAR, AcademicPeriod.IV, null, TestDatabase.TODAY);
        }
        LocalDate monday = LocalDate.of(2026, 10, 5);
        for (int day = 0; day < 5; day++) {
            Map<Integer, AttendanceStatus> marks = new LinkedHashMap<>();
            marks.put(absent.id(), day < 4 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT);
            marks.put(excused.id(), day < 2 ? AttendanceStatus.ABSENT
                    : day < 4 ? AttendanceStatus.MEDICAL_EXCUSE : AttendanceStatus.LATE);
            attendance.recordForGroup(subject.id(), monday.plusDays(day), teacher.id(), marks, false);
        }

        List<AbsenceReportRow> report = attendance.weeklyAbsenceReport(LocalDate.of(2026, 10, 8));

        assertEquals(1, report.stream().filter(row -> row.studentId() == absent.id()).count());
        assertTrue(report.stream().noneMatch(row -> row.studentId() == excused.id()));
        AbsenceReportRow row = report.stream().filter(item -> item.studentId() == absent.id()).findFirst().orElseThrow();
        assertEquals(4, row.absences());
        assertTrue(attendance.weeklyAbsenceReport(LocalDate.of(2026, 10, 13)).stream()
                .noneMatch(item -> item.studentId() == absent.id()), "the next week has no absences");
    }

    @Test
    void attendanceRequiresRegistrationAndConfirmationToCorrect() {
        Teacher teacher = newTeacher();
        Subject subject = newSubject(GRADE_FIFTH);
        teachers.assign(teacher.id(), subject.id(), YEAR, null);
        Student student = newStudent();
        LocalDate day = LocalDate.of(2026, 10, 1);

        assertThrows(BusinessRuleException.class, () -> attendance.record(student.id(), subject.id(), day,
                AttendanceStatus.PRESENT, null, teacher.id(), false));

        enrollments.enroll(student.id(), GRADE_FIFTH, YEAR, LocalDate.of(2026, 1, 20), null);
        academic.register(student.id(), subject.id(), YEAR, AcademicPeriod.IV, null, TestDatabase.TODAY);
        assertFalse(attendance.record(student.id(), subject.id(), day, AttendanceStatus.ABSENT, null, teacher.id(),
                false));
        assertThrows(ConfirmationRequiredException.class, () -> attendance.record(student.id(), subject.id(), day,
                AttendanceStatus.MEDICAL_EXCUSE, null, teacher.id(), false));
        assertTrue(attendance.record(student.id(), subject.id(), day, AttendanceStatus.MEDICAL_EXCUSE, "Incapacidad",
                teacher.id(), true));
        assertThrows(ValidationException.class, () -> attendance.record(student.id(), subject.id(),
                TestDatabase.TODAY.plusDays(1), AttendanceStatus.PRESENT, null, teacher.id(), false));
    }
}
