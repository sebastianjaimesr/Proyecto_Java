package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.common.ImportResult;
import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.util.TextFileReader;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * Catalogue of payment concepts and per-student values (custom value,
 * scholarship, sibling discount).
 */
public class PaymentConceptService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransactionManager transactions;
    private final PaymentConceptDao conceptDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final ChargeCalculator calculator;
    private final Clock clock;

    public PaymentConceptService(TransactionManager transactions, PaymentConceptDao conceptDao, StudentDao studentDao,
                                 GuardianDao guardianDao, ChargeCalculator calculator, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.conceptDao = Objects.requireNonNull(conceptDao, "conceptDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.guardianDao = Objects.requireNonNull(guardianDao, "guardianDao");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<PaymentConcept> findConcepts(boolean onlyActive) {
        return transactions.read(connection -> conceptDao.findAll(connection, onlyActive));
    }

    public PaymentConcept create(PaymentConcept draft) {
        PaymentConcept concept = normalize(draft);
        return transactions.inTransaction(connection -> {
            if (conceptDao.findByCode(connection, concept.code()).isPresent()) {
                throw new BusinessRuleException("Ya existe un concepto con el código " + concept.code() + ".");
            }
            int id = conceptDao.insert(connection, concept);
            return conceptDao.findById(connection, id).orElseThrow();
        });
    }

    /** Updates a concept. Payments already registered keep the amount they were paid with. */
    public PaymentConcept update(PaymentConcept changes) {
        Objects.requireNonNull(changes.id(), "id");
        PaymentConcept concept = normalize(changes);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(conceptDao.findById(connection, concept.id()),
                    "No existe un concepto con id " + concept.id() + ".");
            conceptDao.findByCode(connection, concept.code())
                    .filter(existing -> !existing.id().equals(concept.id()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("El código " + concept.code() + " ya pertenece a otro concepto.");
                    });
            conceptDao.update(connection, concept);
            return conceptDao.findById(connection, concept.id()).orElseThrow();
        });
    }

    /**
     * Registers a custom value, scholarship or sibling discount. A sibling
     * discount requires another active student who shares a guardian.
     */
    public PriceAdjustment addAdjustment(PriceAdjustment draft) {
        PriceAdjustment adjustment = validate(draft);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(studentDao.findById(connection, adjustment.studentId()),
                    "No existe un estudiante con id " + adjustment.studentId() + ".");
            PaymentConcept concept = ServiceChecks.require(conceptDao.findById(connection, adjustment.conceptId()),
                    "No existe un concepto con id " + adjustment.conceptId() + ".");
            if (adjustment.type() == AdjustmentType.SIBLING_DISCOUNT) {
                boolean hasActiveSibling = guardianDao.findSiblings(connection, adjustment.studentId()).stream()
                        .anyMatch(sibling -> sibling.status() == StudentStatus.ACTIVE);
                if (!hasActiveSibling) {
                    throw new BusinessRuleException("El estudiante no tiene hermanos activos en el colegio "
                            + "(estudiantes que compartan acudiente).");
                }
            }
            if (conceptDao.findAdjustment(connection, adjustment.studentId(), adjustment.conceptId(),
                    adjustment.academicYear(), adjustment.type()).isPresent()) {
                throw new BusinessRuleException("El estudiante ya tiene un ajuste de tipo " + adjustment.type().dbValue()
                        + " para " + concept.name() + " en " + adjustment.academicYear() + ".");
            }
            int id = conceptDao.insertAdjustment(connection, adjustment);
            return new PriceAdjustment(id, adjustment.studentId(), adjustment.conceptId(), concept.name(),
                    adjustment.academicYear(), adjustment.type(), adjustment.discountPercent(),
                    adjustment.customAmount(), adjustment.notes());
        });
    }

    public void removeAdjustment(int adjustmentId) {
        transactions.inTransactionVoid(connection -> conceptDao.deleteAdjustment(connection, adjustmentId));
    }

    public List<PriceAdjustment> findAdjustments(int studentId, int academicYear) {
        return transactions.read(connection -> conceptDao.findAdjustments(connection, studentId, academicYear));
    }

    // ------------------------------------------------------ text file exercises

    /** Exercise 20: backup written before the change and the result of applying the file. */
    public record PriceUpdate(Path backup, ImportResult result) {
    }

    /**
     * Exercise 20: writes a backup with the current base value of every
     * concept, then applies codigoConcepto;nuevoValor lines. Unknown codes
     * and negative or non-numeric values are rejected; money is BigDecimal.
     */
    public PriceUpdate updatePricesFromFile(Path file) {
        List<TextFileReader.Line> lines = FileSupport.readDataLines(file, PaymentConceptService.class);
        Path backup = writePriceBackup();
        return new PriceUpdate(backup, applyPrices(lines));
    }

    /** Exercise 20 (bonus): puts back the values of a backup file (same format codigo;valor). */
    public ImportResult restorePrices(Path backupFile) {
        return applyPrices(FileSupport.readDataLines(backupFile, PaymentConceptService.class));
    }

    private Path writePriceBackup() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> lines = new ArrayList<>();
        lines.add("# Respaldo de precios - " + Formats.dateTime(now.withNano(0)));
        lines.add("# codigoConcepto;valorBase (se puede restaurar con la opción del menú)");
        findConcepts(false).forEach(concept -> lines.add(concept.code() + ";" + concept.baseAmount().toPlainString()));
        Path file = FileSupport.EXPORTS.resolve("respaldos").resolve("precios_"
                + now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        return FileSupport.write(file, lines, PaymentConceptService.class);
    }

    private ImportResult applyPrices(List<TextFileReader.Line> lines) {
        Map<String, PaymentConcept> byCode = findConcepts(false).stream()
                .collect(Collectors.toMap(c -> c.code().toUpperCase(Locale.ROOT), Function.identity()));
        List<String> errors = new ArrayList<>();
        int updated = 0;
        for (TextFileReader.Line line : lines) {
            try {
                String[] f = line.fields();
                if (f.length < 2) {
                    throw new ValidationException("se esperaban 2 campos: codigoConcepto;nuevoValor.");
                }
                PaymentConcept old = byCode.get(f[0].toUpperCase(Locale.ROOT));
                if (old == null) {
                    throw new ValidationException("no existe un concepto con el código " + f[0] + ".");
                }
                if (f[1].startsWith("-")) {
                    throw new ValidationException("el valor no puede ser negativo.");
                }
                BigDecimal amount = InputValidator.parseMoney(f[1], "nuevo valor");
                update(new PaymentConcept(old.id(), old.code(), old.name(), old.description(), amount,
                        old.recurring(), old.active()));
                updated++;
            } catch (ValidationException | BusinessRuleException e) {
                errors.add(FileSupport.lineError(line.number(), e.getMessage()));
            }
        }
        return new ImportResult(lines.size(), updated, errors);
    }

    /** Value the student must pay for the concept in that year, after adjustments. */
    public BigDecimal expectedAmount(int studentId, int conceptId, int academicYear) {
        return transactions.read(connection -> {
            PaymentConcept concept = ServiceChecks.require(conceptDao.findById(connection, conceptId),
                    "No existe un concepto con id " + conceptId + ".");
            return calculator.expectedAmount(concept, conceptDao.findAdjustments(connection, studentId, academicYear));
        });
    }

    private PriceAdjustment validate(PriceAdjustment adjustment) {
        if (adjustment.type() == null) {
            throw new ValidationException("El tipo de ajuste es obligatorio.");
        }
        ServiceChecks.academicYear(adjustment.academicYear(), LocalDate.now(clock));
        String notes = InputValidator.optionalText(adjustment.notes(), "observaciones", 1000);
        if (adjustment.type() == AdjustmentType.CUSTOM_VALUE) {
            if (adjustment.customAmount() == null) {
                throw new ValidationException("Indique el valor personalizado.");
            }
            BigDecimal amount = InputValidator.parseMoney(adjustment.customAmount().toPlainString(), "valor personalizado");
            return new PriceAdjustment(null, adjustment.studentId(), adjustment.conceptId(), null,
                    adjustment.academicYear(), adjustment.type(), null, amount, notes);
        }
        BigDecimal percent = adjustment.discountPercent();
        if (percent == null || percent.signum() <= 0 || percent.compareTo(HUNDRED) > 0) {
            throw new ValidationException("El porcentaje de descuento debe ser mayor que 0 y máximo 100.");
        }
        return new PriceAdjustment(null, adjustment.studentId(), adjustment.conceptId(), null, adjustment.academicYear(),
                adjustment.type(), InputValidator.parseDecimal(percent.toPlainString(), "porcentaje", 2), null, notes);
    }

    private static PaymentConcept normalize(PaymentConcept concept) {
        String code = InputValidator.validateCode(concept.code(), "código del concepto", 20);
        String name = InputValidator.requireText(concept.name(), "nombre del concepto", 100);
        if (concept.baseAmount() == null) {
            throw new ValidationException("El valor base es obligatorio.");
        }
        BigDecimal amount = InputValidator.parseMoney(concept.baseAmount().toPlainString(), "valor base");
        return new PaymentConcept(concept.id(), code, name,
                InputValidator.optionalText(concept.description(), "descripción", 2000), amount, concept.recurring(),
                concept.active());
    }
}
