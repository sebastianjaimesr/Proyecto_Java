package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDecimal;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link PaymentConceptDao}.
 */
public class JdbcPaymentConceptDao implements PaymentConceptDao {

    private static final String SELECT = "SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, "
            + "monto_base, es_recurrente, esta_activo FROM conceptos_pago ";
    private static final String SQL_INSERT = "INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, "
            + "descripcion, monto_base, es_recurrente, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE conceptos_pago SET codigo_concepto = ?, nombre_concepto = ?, "
            + "descripcion = ?, monto_base = ?, es_recurrente = ?, esta_activo = ? WHERE id_concepto = ?";

    private static final String ADJUSTMENT_SELECT = "SELECT v.id_valor, v.id_estudiante, v.id_concepto, "
            + "c.nombre_concepto, v.año_academico, v.tipo_ajuste, v.porcentaje_descuento, v.monto_personalizado, "
            + "v.observaciones FROM valores_estudiante_concepto v JOIN conceptos_pago c ON c.id_concepto = v.id_concepto ";
    private static final String SQL_INSERT_ADJUSTMENT = "INSERT INTO valores_estudiante_concepto (id_estudiante, "
            + "id_concepto, año_academico, tipo_ajuste, porcentaje_descuento, monto_personalizado, observaciones) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_DELETE_ADJUSTMENT = "DELETE FROM valores_estudiante_concepto WHERE id_valor = ?";

    @Override
    public int insert(Connection connection, PaymentConcept concept) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, concept));
    }

    @Override
    public void update(Connection connection, PaymentConcept concept) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, concept);
            statement.setInt(7, concept.id());
        });
    }

    @Override
    public Optional<PaymentConcept> findById(Connection connection, int conceptId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE id_concepto = ?",
                statement -> statement.setInt(1, conceptId), JdbcPaymentConceptDao::map);
    }

    @Override
    public Optional<PaymentConcept> findByCode(Connection connection, String code) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE codigo_concepto = ?",
                statement -> statement.setString(1, code), JdbcPaymentConceptDao::map);
    }

    @Override
    public List<PaymentConcept> findAll(Connection connection, boolean onlyActive) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE ? = FALSE OR esta_activo = TRUE ORDER BY nombre_concepto",
                statement -> statement.setBoolean(1, onlyActive), JdbcPaymentConceptDao::map);
    }

    @Override
    public int insertAdjustment(Connection connection, PriceAdjustment adjustment) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT_ADJUSTMENT, statement -> {
            statement.setInt(1, adjustment.studentId());
            statement.setInt(2, adjustment.conceptId());
            statement.setInt(3, adjustment.academicYear());
            setEnum(statement, 4, adjustment.type());
            setDecimal(statement, 5, adjustment.discountPercent());
            setDecimal(statement, 6, adjustment.customAmount());
            setString(statement, 7, adjustment.notes());
        });
    }

    @Override
    public Optional<PriceAdjustment> findAdjustment(Connection connection, int studentId, int conceptId,
                                                    int academicYear, AdjustmentType type) throws SQLException {
        return JdbcSupport.queryOne(connection, ADJUSTMENT_SELECT + "WHERE v.id_estudiante = ? AND v.id_concepto = ? "
                + "AND v.año_academico = ? AND v.tipo_ajuste = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, conceptId);
                    statement.setInt(3, academicYear);
                    setEnum(statement, 4, type);
                }, JdbcPaymentConceptDao::mapAdjustment);
    }

    @Override
    public List<PriceAdjustment> findAdjustments(Connection connection, int studentId, int academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection, ADJUSTMENT_SELECT
                + "WHERE v.id_estudiante = ? AND v.año_academico = ? ORDER BY c.nombre_concepto, v.tipo_ajuste",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, academicYear);
                }, JdbcPaymentConceptDao::mapAdjustment);
    }

    @Override
    public void deleteAdjustment(Connection connection, int adjustmentId) throws SQLException {
        JdbcSupport.update(connection, SQL_DELETE_ADJUSTMENT, statement -> statement.setInt(1, adjustmentId));
    }

    private static void bindFields(PreparedStatement statement, PaymentConcept concept) throws SQLException {
        statement.setString(1, concept.code());
        statement.setString(2, concept.name());
        setString(statement, 3, concept.description());
        setDecimal(statement, 4, concept.baseAmount());
        statement.setBoolean(5, concept.recurring());
        statement.setBoolean(6, concept.active());
    }

    private static PaymentConcept map(ResultSet resultSet) throws SQLException {
        return new PaymentConcept(
                resultSet.getInt("id_concepto"),
                resultSet.getString("codigo_concepto"),
                resultSet.getString("nombre_concepto"),
                resultSet.getString("descripcion"),
                resultSet.getBigDecimal("monto_base"),
                resultSet.getBoolean("es_recurrente"),
                resultSet.getBoolean("esta_activo"));
    }

    private static PriceAdjustment mapAdjustment(ResultSet resultSet) throws SQLException {
        return new PriceAdjustment(
                resultSet.getInt("id_valor"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_concepto"),
                resultSet.getString("nombre_concepto"),
                resultSet.getInt("año_academico"),
                AdjustmentType.fromDbValue(resultSet.getString("tipo_ajuste")),
                resultSet.getBigDecimal("porcentaje_descuento"),
                resultSet.getBigDecimal("monto_personalizado"),
                resultSet.getString("observaciones"));
    }
}
