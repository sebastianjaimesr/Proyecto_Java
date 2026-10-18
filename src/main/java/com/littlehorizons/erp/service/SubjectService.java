package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.CatalogDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Subjects catalogue per grade.
 */
public class SubjectService {

    static final int MIN_WEEKLY_HOURS = 1;
    static final int MAX_WEEKLY_HOURS = 20;
    private static final int MAX_CODE_LENGTH = 20;
    private static final int MAX_NAME_LENGTH = 100;

    private final TransactionManager transactions;
    private final SubjectDao subjectDao;
    private final CatalogDao catalogDao;

    public SubjectService(TransactionManager transactions, SubjectDao subjectDao, CatalogDao catalogDao) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.catalogDao = Objects.requireNonNull(catalogDao, "catalogDao");
    }

    public Subject register(Subject draft) {
        Subject subject = normalize(draft);
        return transactions.inTransaction(connection -> {
            Grade grade = requireGrade(connection, subject.gradeId());
            ensureCodeIsFree(connection, subject.code(), null);
            Subject saved = subject.withId(subjectDao.insert(connection, subject));
            return new Subject(saved.id(), saved.code(), saved.name(), saved.description(), grade.id(),
                    grade.name(), saved.weeklyHours(), saved.active());
        });
    }

    public Subject update(Subject changes) {
        Objects.requireNonNull(changes.id(), "id");
        Subject subject = normalize(changes);
        return transactions.inTransaction(connection -> {
            Subject current = ServiceChecks.require(subjectDao.findById(connection, subject.id()),
                    "No existe una materia con id " + subject.id() + ".");
            requireGrade(connection, subject.gradeId());
            if (current.gradeId() != subject.gradeId()) {
                throw new BusinessRuleException("No se puede cambiar el grado de una materia existente porque "
                        + "tiene historial asociado; registre una materia nueva para el otro grado.");
            }
            ensureCodeIsFree(connection, subject.code(), subject.id());
            subjectDao.update(connection, subject);
            return subject;
        });
    }

    public Subject findById(int subjectId) {
        return transactions.read(connection -> ServiceChecks.require(subjectDao.findById(connection, subjectId),
                "No existe una materia con id " + subjectId + "."));
    }

    public List<Subject> findByGrade(int gradeId, boolean onlyActive) {
        return transactions.read(connection -> {
            requireGrade(connection, gradeId);
            return subjectDao.findByGrade(connection, gradeId, onlyActive);
        });
    }

    public List<Subject> findAll() {
        return transactions.read(subjectDao::findAll);
    }

    public List<Grade> findGrades() {
        return transactions.read(catalogDao::findAllGrades);
    }

    private Grade requireGrade(Connection connection, int gradeId) throws SQLException {
        return ServiceChecks.require(catalogDao.findGradeById(connection, gradeId),
                "No existe un grado con id " + gradeId + ".");
    }

    private void ensureCodeIsFree(Connection connection, String code, Integer ownId) throws SQLException {
        subjectDao.findByCode(connection, code)
                .filter(existing -> !existing.id().equals(ownId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Ya existe una materia con el código " + code
                            + " (" + existing.name() + ").");
                });
    }

    private static Subject normalize(Subject subject) {
        String code = InputValidator.validateCode(subject.code(), "código de materia", MAX_CODE_LENGTH);
        String name = InputValidator.requireText(subject.name(), "nombre de la materia", MAX_NAME_LENGTH);
        if (subject.weeklyHours() < MIN_WEEKLY_HOURS || subject.weeklyHours() > MAX_WEEKLY_HOURS) {
            throw new com.littlehorizons.erp.exception.ValidationException("Las horas semanales deben estar entre "
                    + MIN_WEEKLY_HOURS + " y " + MAX_WEEKLY_HOURS + ".");
        }
        String description = subject.description() == null || subject.description().isBlank()
                ? null : subject.description().strip();
        return new Subject(subject.id(), code, name, description, subject.gradeId(), subject.gradeName(),
                subject.weeklyHours(), subject.active());
    }
}
