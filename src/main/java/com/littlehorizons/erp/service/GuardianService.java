package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.StudentLink;
import com.littlehorizons.erp.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Reglas de negocio de los acudientes y su relación con los estudiantes (N:M).
 */
public class GuardianService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_RELATIONSHIP_LENGTH = 50;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int SEARCH_LIMIT = 100;

    private final TransactionManager transactions;
    private final GuardianDao guardianDao;
    private final StudentDao studentDao;

    public GuardianService(TransactionManager transactions, GuardianDao guardianDao, StudentDao studentDao) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.guardianDao = Objects.requireNonNull(guardianDao, "guardianDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
    }

    public Guardian register(Guardian draft) {
        Guardian guardian = normalize(draft);
        validate(guardian);
        return transactions.inTransaction(connection -> {
            ensureDocumentIsFree(connection, guardian.documentNumber(), null);
            return guardian.withId(guardianDao.insert(connection, guardian));
        });
    }

    /**
     * Registra un acudiente y lo vincula a un estudiante en una sola transacción, así un
     * acudiente nunca queda sin el estudiante para el que se creó.
     */
    public Guardian registerForStudent(Guardian draft, int studentId, boolean primary, boolean emergencyContact) {
        Guardian guardian = normalize(draft);
        validate(guardian);
        return transactions.inTransaction(connection -> {
            requireStudent(connection, studentId);
            ensureDocumentIsFree(connection, guardian.documentNumber(), null);
            Guardian saved = guardian.withId(guardianDao.insert(connection, guardian));
            if (primary) {
                guardianDao.clearPrimary(connection, studentId);
            }
            guardianDao.link(connection, studentId, saved.id(), primary, emergencyContact);
            return saved;
        });
    }

    public Guardian update(Guardian changes) {
        Objects.requireNonNull(changes.id(), "id");
        Guardian guardian = normalize(changes);
        validate(guardian);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(guardianDao.findById(connection, guardian.id()),
                    "No existe un acudiente con id " + guardian.id() + ".");
            ensureDocumentIsFree(connection, guardian.documentNumber(), guardian.id());
            guardianDao.update(connection, guardian);
            return guardian;
        });
    }

    /** Vincula un acudiente existente; si es {@code primary}, reemplaza al acudiente principal actual. */
    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        transactions.inTransactionVoid(connection -> {
            requireStudent(connection, studentId);
            requireGuardian(connection, guardianId);
            boolean linked = guardianDao.isLinked(connection, studentId, guardianId);
            if (primary) {
                guardianDao.clearPrimary(connection, studentId);
            }
            if (linked) {
                guardianDao.updateLink(connection, studentId, guardianId, primary, emergencyContact);
            } else {
                guardianDao.link(connection, studentId, guardianId, primary, emergencyContact);
            }
        });
    }

    /** Elimina la relación; un estudiante debe conservar al menos un acudiente. */
    public void unlink(int studentId, int guardianId) {
        transactions.inTransactionVoid(connection -> {
            if (!guardianDao.isLinked(connection, studentId, guardianId)) {
                throw new BusinessRuleException("El acudiente no está relacionado con ese estudiante.");
            }
            if (guardianDao.findGuardiansOfStudent(connection, studentId).size() <= 1) {
                throw new BusinessRuleException("El estudiante debe conservar al menos un acudiente.");
            }
            guardianDao.unlink(connection, studentId, guardianId);
        });
    }

    public Guardian findById(int guardianId) {
        return transactions.read(connection -> requireGuardian(connection, guardianId));
    }

    public List<Guardian> search(String text) {
        return transactions.read(connection -> guardianDao.search(connection, text, SEARCH_LIMIT));
    }

    public List<GuardianLink> findGuardiansOfStudent(int studentId) {
        return transactions.read(connection -> {
            requireStudent(connection, studentId);
            return guardianDao.findGuardiansOfStudent(connection, studentId);
        });
    }

    public List<StudentLink> findStudentsOfGuardian(int guardianId) {
        return transactions.read(connection -> {
            requireGuardian(connection, guardianId);
            return guardianDao.findStudentsOfGuardian(connection, guardianId);
        });
    }

    private void requireStudent(Connection connection, int studentId) throws SQLException {
        ServiceChecks.require(studentDao.findById(connection, studentId),
                "No existe un estudiante con id " + studentId + ".");
    }

    private Guardian requireGuardian(Connection connection, int guardianId) throws SQLException {
        return ServiceChecks.require(guardianDao.findById(connection, guardianId),
                "No existe un acudiente con id " + guardianId + ".");
    }

    private void ensureDocumentIsFree(Connection connection, String documentNumber, Integer ownId)
            throws SQLException {
        guardianDao.findByDocument(connection, documentNumber)
                .filter(existing -> !existing.id().equals(ownId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Ya existe un acudiente con el documento " + documentNumber
                            + " (" + existing.fullName() + ").");
                });
    }

    private static Guardian normalize(Guardian guardian) {
        return new Guardian(guardian.id(), guardian.documentType(),
                InputValidator.validateDocumentNumber(guardian.documentNumber()), guardian.name(),
                guardian.relationship() == null ? null : guardian.relationship().strip(),
                guardian.mainPhone() == null ? null : guardian.mainPhone().strip(),
                blankToNull(guardian.secondaryPhone()),
                guardian.email() == null || guardian.email().isBlank()
                        ? null : InputValidator.validateEmail(guardian.email(), "correo electrónico"),
                blankToNull(guardian.address()), blankToNull(guardian.occupation()),
                guardian.financialResponsible());
    }

    private static void validate(Guardian guardian) {
        if (guardian.documentType() == null) {
            throw new ValidationException("El tipo de documento es obligatorio.");
        }
        ServiceChecks.requireText(guardian.name().firstName(), "primer nombre", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(guardian.name().middleName(), "segundo nombre", MAX_NAME_LENGTH);
        ServiceChecks.requireText(guardian.name().lastName(), "primer apellido", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(guardian.name().secondLastName(), "segundo apellido", MAX_NAME_LENGTH);
        ServiceChecks.requireText(guardian.relationship(), "parentesco", MAX_RELATIONSHIP_LENGTH);
        InputValidator.validatePhone(guardian.mainPhone(), "teléfono principal");
        ServiceChecks.optionalPhone(guardian.secondaryPhone(), "teléfono secundario");
        ServiceChecks.optionalText(guardian.address(), "dirección", MAX_ADDRESS_LENGTH);
        ServiceChecks.optionalText(guardian.occupation(), "ocupación", MAX_NAME_LENGTH);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
