package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
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
