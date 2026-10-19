package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code conceptos_pago} y de los ajustes por estudiante en
 * {@code valores_estudiante_concepto} (cambio autorizado P-2).
 */
public interface PaymentConceptDao {

    int insert(Connection connection, PaymentConcept concept) throws SQLException;

    void update(Connection connection, PaymentConcept concept) throws SQLException;

    Optional<PaymentConcept> findById(Connection connection, int conceptId) throws SQLException;

    Optional<PaymentConcept> findByCode(Connection connection, String code) throws SQLException;

    List<PaymentConcept> findAll(Connection connection, boolean onlyActive) throws SQLException;

    // ------------------------------------------------------- ajustes

    int insertAdjustment(Connection connection, PriceAdjustment adjustment) throws SQLException;

    Optional<PriceAdjustment> findAdjustment(Connection connection, int studentId, int conceptId, int academicYear,
                                             AdjustmentType type) throws SQLException;

    /** Ajustes de un estudiante en un año (todos los conceptos). */
    List<PriceAdjustment> findAdjustments(Connection connection, int studentId, int academicYear) throws SQLException;

    /** Elimina un ajuste; los pagos ya realizados no se ven afectados. */
    void deleteAdjustment(Connection connection, int adjustmentId) throws SQLException;
}
