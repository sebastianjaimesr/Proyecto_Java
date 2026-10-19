package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcLoanDao;
import com.littlehorizons.erp.dao.jdbc.JdbcResourceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherAssignmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;
import com.littlehorizons.erp.model.resources.LoanStatus;
import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceStatus;
import com.littlehorizons.erp.support.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inventario, préstamos, devoluciones y préstamos vencidos contra la base MySQL de pruebas.
 */
class ResourceIntegrationTest {

    private static final int CATEGORY_BOOK = 1;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1300);

    private static StudentService students;
    private static TeacherService teachers;
    private static ResourceService resources;
    private static LoanService loans;

    @BeforeAll
    static void createDatabase() {
        TestDatabase database = TestDatabase.recreate();
        TransactionManager transactions = database.transactionManager();
        JdbcStudentDao studentDao = new JdbcStudentDao();
        JdbcTeacherDao teacherDao = new JdbcTeacherDao();
        JdbcResourceDao resourceDao = new JdbcResourceDao();
        JdbcLoanDao loanDao = new JdbcLoanDao();
        students = new StudentService(transactions, studentDao, new JdbcGuardianDao(), new JdbcEnrollmentDao(),
                TestDatabase.CLOCK);
        teachers = new TeacherService(transactions, teacherDao, new JdbcSubjectDao(), new JdbcTeacherAssignmentDao(),
                TestDatabase.CLOCK);
        resources = new ResourceService(transactions, resourceDao, loanDao, TestDatabase.CLOCK);
        loans = new LoanService(transactions, loanDao, resourceDao, studentDao, teacherDao, TestDatabase.CLOCK);
    }

    private static Resource newResource() {
        int n = SEQUENCE.incrementAndGet();
        return resources.register(new Resource(null, "LIB-" + n, "Cien años de soledad " + n, CATEGORY_BOOK, null,
                null, LocalDate.of(2024, 3, 1), "Biblioteca", null));
    }

    private static Student newStudent() {
        int n = SEQUENCE.incrementAndGet();
        return students.register(new Student(null, StudentDocumentType.TI, "TI" + (4_000_000 + n),
                new PersonName("Samuel", null, "Lector" + n, null), LocalDate.of(2012, 8, 8), Gender.MALE, null, null,
                null, null, null, null, LocalDate.of(2019, 1, 20), StudentStatus.ACTIVE));
    }

    private static Teacher newTeacher() {
        int n = SEQUENCE.incrementAndGet();
        return teachers.register(new Teacher(null, PersonDocumentType.CC, "CC" + (80_000_000 + n),
                new PersonName("Diana", null, "Docente" + n, null), "diana" + n + "@littlehorizons.edu.co", null,
                "Ciencias", LocalDate.of(2020, 1, 15), TeacherStatus.ACTIVE));
    }

    @Test
    void duplicateResourceCodeIsRejected() {
        Resource resource = newResource();
        assertThrows(BusinessRuleException.class, () -> resources.register(new Resource(null, resource.code(), "Otro",
                CATEGORY_BOOK, null, null, null, null, null)));
    }

    @Test
    @DisplayName("Lending and returning update the loan and the resource atomically")
    void loanAndReturnUpdateResourceStatus() {
        Resource resource = newResource();
        Student student = newStudent();

        Loan loan = loans.lend(BorrowerType.STUDENT, resource.id(), student.id(), LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10), null);

        assertEquals(LoanStatus.ACTIVE, loan.status());
        assertEquals(ResourceStatus.LOANED, resources.findByCode(resource.code()).status());

        Loan returned = loans.returnLoan(BorrowerType.STUDENT, loan.id(), LocalDate.of(2026, 10, 9), "En buen estado");

        assertEquals(LoanStatus.RETURNED, returned.status());
        assertEquals(LocalDate.of(2026, 10, 9), returned.returnDate());
        assertEquals(ResourceStatus.AVAILABLE, resources.findByCode(resource.code()).status());
        assertThrows(BusinessRuleException.class,
                () -> loans.returnLoan(BorrowerType.STUDENT, loan.id(), TestDatabase.TODAY, null));
    }

    @Test
    void unavailableResourcesCannotBeLentAcrossBothTables() {
        Resource resource = newResource();
        Student student = newStudent();
        Teacher teacher = newTeacher();
        loans.lend(BorrowerType.TEACHER, resource.id(), teacher.id(), LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 20), null);

        assertThrows(BusinessRuleException.class, () -> loans.lend(BorrowerType.STUDENT, resource.id(), student.id(),
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5), null));

        Resource broken = newResource();
        resources.update(broken.withStatus(ResourceStatus.MAINTENANCE));
        assertThrows(BusinessRuleException.class, () -> loans.lend(BorrowerType.STUDENT, broken.id(), student.id(),
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5), null));
        assertThrows(BusinessRuleException.class, () -> resources.update(
                resources.findByCode(resource.code()).withStatus(ResourceStatus.RETIRED)),
                "a loaned resource cannot be retired");
    }

    @Test
    void invalidLoanDatesAreRejected() {
        Resource resource = newResource();
        Student student = newStudent();
        assertThrows(ValidationException.class, () -> loans.lend(BorrowerType.STUDENT, resource.id(), student.id(),
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 5), null));
        assertThrows(ValidationException.class, () -> loans.lend(BorrowerType.STUDENT, resource.id(), student.id(),
                TestDatabase.TODAY.plusDays(1), TestDatabase.TODAY.plusDays(5), null));
        assertThrows(ValidationException.class, () -> loans.lend(BorrowerType.STUDENT, resource.id(), student.id(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), null));
        assertEquals(ResourceStatus.AVAILABLE, resources.findByCode(resource.code()).status());
    }

    @Test
    @DisplayName("Overdue loans are reported and marked as Vencido")
    void overdueLoansAreReported() {
        Resource lateBook = newResource();
        Resource onTimeBook = newResource();
        Student student = newStudent();
        Teacher teacher = newTeacher();
        Loan late = loans.lend(BorrowerType.STUDENT, lateBook.id(), student.id(), LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 10, 5), null);
        loans.lend(BorrowerType.TEACHER, onTimeBook.id(), teacher.id(), LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 30), null);

        List<Loan> overdue = loans.findOverdue();

        Loan reported = overdue.stream().filter(loan -> loan.id().equals(late.id())
                && loan.borrowerType() == BorrowerType.STUDENT).findFirst().orElseThrow();
        assertEquals(10, loans.daysOverdue(reported));
        assertTrue(overdue.stream().noneMatch(loan -> loan.resourceId() == onTimeBook.id()));
        assertTrue(loans.refreshOverdueStatus() >= 1);
        assertEquals(LoanStatus.OVERDUE, loans.findByBorrower(BorrowerType.STUDENT, student.id()).get(0).status());

        Loan returned = loans.returnLoan(BorrowerType.STUDENT, late.id(), TestDatabase.TODAY, null);
        assertEquals(LoanStatus.RETURNED, returned.status());
        assertEquals(ResourceStatus.AVAILABLE, resources.findByCode(lateBook.code()).status());
    }
}
