package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;
import com.littlehorizons.erp.model.resources.LoanStatus;
import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceStatus;
import com.littlehorizons.erp.model.common.FileReport;
import com.littlehorizons.erp.util.FileNames;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.util.TextFileReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loans and returns of resources to students and teachers.
 *
 * <p>Each loan and each return changes two tables (the loan and the
 * resource status) in one transaction. The resource row is locked first with
 * {@code SELECT ... FOR UPDATE}, so the same resource cannot be lent twice
 * at the same time, even across the two loan tables.</p>
 */
public class LoanService {

    public static final int MAX_LOAN_DAYS = 90;
    static final String LOAN_OPERATION = "PRÉSTAMO";
    static final String RETURN_OPERATION = "DEVOLUCIÓN";

    private final TransactionManager transactions;
    private final LoanDao loanDao;
    private final ResourceDao resourceDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final Clock clock;

    public LoanService(TransactionManager transactions, LoanDao loanDao, ResourceDao resourceDao, StudentDao studentDao,
                       TeacherDao teacherDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.loanDao = Objects.requireNonNull(loanDao, "loanDao");
        this.resourceDao = Objects.requireNonNull(resourceDao, "resourceDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Loan lend(BorrowerType type, int resourceId, int borrowerId, LocalDate loanDate, LocalDate dueDate,
                     String notes) {
        Objects.requireNonNull(type, "type");
        LocalDate today = LocalDate.now(clock);
        ServiceChecks.notInFuture(loanDate, today, "fecha de préstamo");
        if (dueDate == null || dueDate.isBefore(loanDate)) {
            throw new ValidationException("La fecha de devolución debe ser igual o posterior a la fecha de préstamo.");
        }
        if (ChronoUnit.DAYS.between(loanDate, dueDate) > MAX_LOAN_DAYS) {
            throw new ValidationException("Un préstamo puede durar como máximo " + MAX_LOAN_DAYS + " días.");
        }
        String cleanNotes = InputValidator.optionalText(notes, "observaciones", 1000);
        Loan loan = transactions.inTransaction(connection -> {
            Resource resource = ServiceChecks.require(resourceDao.lockById(connection, resourceId),
                    "No existe un recurso con id " + resourceId + ".");
            if (resource.status() != ResourceStatus.AVAILABLE) {
                throw new BusinessRuleException("El recurso " + resource.code() + " no está disponible (estado: "
                        + resource.status().dbValue() + ").");
            }
            if (!loanDao.findOpenByResource(connection, resourceId).isEmpty()) {
                throw new BusinessRuleException("El recurso " + resource.code() + " tiene un préstamo sin devolver.");
            }
            String borrowerName = requireActiveBorrower(connection, type, borrowerId);
            int id = loanDao.insert(connection, new Loan(null, type, resourceId, resource.code(), resource.name(),
                    borrowerId, borrowerName, loanDate, dueDate, null, LoanStatus.ACTIVE, cleanNotes));
            resourceDao.updateStatus(connection, resourceId, ResourceStatus.LOANED);
            return loanDao.findById(connection, type, id).orElseThrow();
        });
        appendToJournal(LOAN_OPERATION, loan); // after the commit: a failed loan leaves no line
        return loan;
    }

    /** Registers the return and makes the resource available again, atomically. */
    public Loan returnLoan(BorrowerType type, int loanId, LocalDate returnDate, String notes) {
        Objects.requireNonNull(type, "type");
        ServiceChecks.notInFuture(returnDate, LocalDate.now(clock), "fecha de devolución");
        String cleanNotes = InputValidator.optionalText(notes, "observaciones", 1000);
        Loan returned = transactions.inTransaction(connection -> {
            Loan loan = ServiceChecks.require(loanDao.findById(connection, type, loanId),
                    "No existe un préstamo a " + type.label().toLowerCase(Locale.ROOT) + " con id " + loanId + ".");
            if (loan.status() == LoanStatus.RETURNED) {
                throw new BusinessRuleException("El préstamo ya fue devuelto el " + loan.returnDate() + ".");
            }
            if (returnDate.isBefore(loan.loanDate())) {
                throw new ValidationException("La devolución no puede ser anterior al préstamo (" + loan.loanDate() + ").");
            }
            resourceDao.lockById(connection, loan.resourceId());
            loanDao.markReturned(connection, type, loanId, returnDate, cleanNotes == null ? loan.notes() : cleanNotes);
            resourceDao.updateStatus(connection, loan.resourceId(), ResourceStatus.AVAILABLE);
            return loanDao.findById(connection, type, loanId).orElseThrow();
        });
        appendToJournal(RETURN_OPERATION, returned);
        return returned;
    }

    /** Marks as "Vencido" every active loan past its due date; returns how many changed. */
    public int refreshOverdueStatus() {
        LocalDate today = LocalDate.now(clock);
        return transactions.inTransaction(connection -> loanDao.markOverdue(connection, today));
    }

    /** Loans not returned and past their due date, most overdue first. */
    public List<Loan> findOverdue() {
        LocalDate today = LocalDate.now(clock);
        return transactions.read(loanDao::findOpen).stream()
                .filter(loan -> loan.isOverdueOn(today))
                .sorted(Comparator.comparing(Loan::dueDate))
                .collect(Collectors.toList());
    }

    public List<Loan> findOpen() {
        return transactions.read(loanDao::findOpen);
    }

    public List<Loan> findByBorrower(BorrowerType type, int borrowerId) {
        return transactions.read(connection -> loanDao.findByBorrower(connection, type, borrowerId));
    }

    public long daysOverdue(Loan loan) {
        return loan.daysOverdue(LocalDate.now(clock));
    }

    // ------------------------------------------------------ text file exercises

    /** Exercise 13: loan journal, in the logs folder (logs.dir). */
    public static Path journalFile() {
        return FileSupport.logsFolder().resolve("bitacora_prestamos.txt");
    }

    /** Exercise 13 (a): fecha;operación;código;quién, appended after each loan or return. */
    private void appendToJournal(String operation, Loan loan) {
        String line = String.join(";", Formats.dateTime(LocalDateTime.now(clock).withNano(0)), operation,
                loan.resourceCode(), loan.borrowerType().label() + " " + loan.borrowerName());
        FileSupport.appendQuietly(journalFile(), List.of(line), LoanService.class);
    }

    /**
     * Exercise 13 (b): movements of one resource read from the journal with
     * BufferedReader, most recent first. Empty when the journal does not exist yet.
     */
    public List<String> journalOf(String resourceCode) {
        String code = InputValidator.requireText(resourceCode, "código del recurso", 50);
        Path file = journalFile();
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> movements = FileSupport.readAllLines(file, LoanService.class).stream()
                .filter(line -> {
                    String[] fields = new TextFileReader.Line(0, line).fields();
                    return fields.length >= 3 && fields[2].equalsIgnoreCase(code);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(movements);
        return movements;
    }

    /**
     * Exercise 11: one notice per borrower with overdue loans (several loans,
     * one notice), in exports/avisos, named aviso_&lt;tipo&gt;_&lt;id&gt;.txt.
     */
    public FileReport writeOverdueNotices() {
        LocalDate today = LocalDate.now(clock);
        Map<String, List<Loan>> byBorrower = findOverdue().stream()
                .collect(Collectors.groupingBy(loan -> FileNames.safe(loan.borrowerType().label()) + "_"
                        + loan.borrowerId(), LinkedHashMap::new, Collectors.toList()));
        if (byBorrower.isEmpty()) {
            throw new BusinessRuleException("No hay préstamos vencidos; no se generaron avisos.");
        }
        Path folder = FileSupport.EXPORTS.resolve("avisos");
        List<Path> files = new ArrayList<>();
        byBorrower.forEach((key, loans) -> {
            Loan first = loans.get(0);
            List<String> lines = new ArrayList<>();
            lines.add("COLEGIO INTERNACIONAL LITTLE HORIZONS - Biblioteca y recursos");
            lines.add("Aviso de préstamos vencidos - " + Formats.longDate(today));
            lines.add("");
            lines.add(first.borrowerType().label() + ": " + first.borrowerName());
            lines.add("");
            lines.add(String.format("  %-12s %-32s %-11s %-11s %6s", "Código", "Recurso", "Prestado", "Esperado",
                    "Días"));
            loans.forEach(loan -> lines.add(String.format("  %-12s %-32s %-11s %-11s %6d", loan.resourceCode(),
                    FileSupport.fit(loan.resourceName(), 32), Formats.date(loan.loanDate()), Formats.date(loan.dueDate()),
                    loan.daysOverdue(today))));
            lines.add("");
            lines.add("Por favor devuelva los recursos lo antes posible.");
            files.add(FileSupport.write(folder.resolve("aviso_" + key + ".txt"), lines, LoanService.class));
        });
        return new FileReport(folder, files);
    }

    private String requireActiveBorrower(Connection connection, BorrowerType type, int borrowerId)
            throws SQLException {
        if (type == BorrowerType.STUDENT) {
            Student student = ServiceChecks.require(studentDao.findById(connection, borrowerId),
                    "No existe un estudiante con id " + borrowerId + ".");
            if (student.status() != StudentStatus.ACTIVE) {
                throw new BusinessRuleException("Solo se presta a estudiantes activos.");
            }
            return student.fullName();
        }
        Teacher teacher = ServiceChecks.require(teacherDao.findById(connection, borrowerId),
                "No existe un profesor con id " + borrowerId + ".");
        if (teacher.status() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Solo se presta a profesores activos.");
        }
        return teacher.fullName();
    }
}
