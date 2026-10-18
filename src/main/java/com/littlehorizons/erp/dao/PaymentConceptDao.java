package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code conceptos_pago} and of the per-student adjustments in
 * {@code valores_estudiante_concepto} (authorized change P-2).
 */
public interface PaymentConceptDao {

    int insert(Connection connection, PaymentConcept concept) throws SQLException;

    void update(Connection connection, PaymentConcept concept) throws SQLException;

    Optional<PaymentConcept> findById(Connection connection, int conceptId) throws SQLException;

    Optional<PaymentConcept> findByCode(Connection connection, String code) throws SQLException;

    List<PaymentConcept> findAll(Connection connection, boolean onlyActive) throws SQLException;

    // ------------------------------------------------------- adjustments

    int insertAdjustment(Connection connection, PriceAdjustment adjustment) throws SQLException;

    Optional<PriceAdjustment> findAdjustment(Connection connection, int studentId, int conceptId, int academicYear,
                                             AdjustmentType type) throws SQLException;

    /** Adjustments of a student in a year (all concepts). */
    List<PriceAdjustment> findAdjustments(Connection connection, int studentId, int academicYear) throws SQLException;

    /** Removes an adjustment; payments already made are not affected. */
    void deleteAdjustment(Connection connection, int adjustmentId) throws SQLException;
}
