package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentMethod;
import com.littlehorizons.erp.model.finance.PaymentPeriod;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDecimal;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * JDBC implementation of {@link PaymentDao}.
 */
public class JdbcPaymentDao implements PaymentDao {

    private static final String SELECT = "SELECT pg.id_pago, pg.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre_estudiante, "
            + "pg.id_acudiente, CONCAT_WS(' ', a.primer_nombre, a.primer_apellido, a.segundo_apellido) AS nombre_acudiente, "
            + "pg.id_concepto, c.codigo_concepto, c.nombre_concepto, pg.monto_pagado, pg.fecha_pago, pg.metodo_pago, "
            + "pg.numero_recibo, pg.año_academico, pg.periodo_academico, pg.notas "
            + "FROM pagos pg JOIN estudiantes e ON e.id_estudiante = pg.id_estudiante "
            + "JOIN conceptos_pago c ON c.id_concepto = pg.id_concepto "
            + "LEFT JOIN acudientes a ON a.id_acudiente = pg.id_acudiente ";
    private static final String ORDER = " ORDER BY pg.fecha_pago DESC, pg.id_pago DESC";
    private static final String SQL_INSERT = "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, "
            + "monto_pagado, fecha_pago, metodo_pago, numero_recibo, año_academico, periodo_academico, notas) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SET_RECEIPT = "UPDATE pagos SET numero_recibo = ? WHERE id_pago = ?";

    /**
     * The main guardian is the financial responsible marked as principal; if
     * none is principal, the financial responsible with the lowest id.
     */
    private static final String SQL_BILLABLE = "SELECT e.id_estudiante, e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) AS nombre_estudiante, "
            + "g.nombre_grado, m.fecha_matricula, "
            + "(SELECT CONCAT_WS(' ', a.primer_nombre, a.primer_apellido) FROM estudiante_acudiente ea "
            + " JOIN acudientes a ON a.id_acudiente = ea.id_acudiente WHERE ea.id_estudiante = e.id_estudiante "
            + " AND a.es_responsable_financiero = TRUE ORDER BY ea.es_principal DESC, a.id_acudiente LIMIT 1) AS acudiente, "
            + "(SELECT a.telefono_principal FROM estudiante_acudiente ea "
            + " JOIN acudientes a ON a.id_acudiente = ea.id_acudiente WHERE ea.id_estudiante = e.id_estudiante "
            + " AND a.es_responsable_financiero = TRUE ORDER BY ea.es_principal DESC, a.id_acudiente LIMIT 1) AS telefono "
            + "FROM matriculas m JOIN estudiantes e ON e.id_estudiante = m.id_estudiante "
            + "JOIN grados g ON g.id_grado = m.id_grado "
            + "WHERE m.año_academico = ? AND m.estado_matricula = 'Activa' "
            + "ORDER BY g.nivel_numerico, e.primer_apellido, e.primer_nombre";

    @Override
    public int insert(Connection connection, Payment payment) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setInt(1, payment.studentId());
            setInteger(statement, 2, payment.guardianId());
            statement.setInt(3, payment.conceptId());
            setDecimal(statement, 4, payment.amount());
            setDate(statement, 5, payment.paymentDate());
            setEnum(statement, 6, payment.method());
            setString(statement, 7, payment.receiptNumber());
            setInteger(statement, 8, payment.academicYear());
            setEnum(statement, 9, payment.period());
            setString(statement, 10, payment.notes());
        });
    }

    @Override
    public void setReceiptNumber(Connection connection, int paymentId, String receiptNumber) throws SQLException {
        JdbcSupport.update(connection, SQL_SET_RECEIPT, statement -> {
            statement.setString(1, receiptNumber);
            statement.setInt(2, paymentId);
        });
    }

    @Override
    public Optional<Payment> findById(Connection connection, int paymentId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE pg.id_pago = ?",
                statement -> statement.setInt(1, paymentId), JdbcPaymentDao::map);
    }

    @Override
    public Optional<Payment> findByReceipt(Connection connection, String receiptNumber) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE pg.numero_recibo = ?",
                statement -> statement.setString(1, receiptNumber), JdbcPaymentDao::map);
    }

    @Override
    public Optional<Payment> findSimilar(Connection connection, int studentId, int conceptId, BigDecimal amount,
                                         LocalDate date, PaymentMethod method) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE pg.id_estudiante = ? AND pg.id_concepto = ? "
                + "AND pg.monto_pagado = ? AND pg.fecha_pago = ? AND pg.metodo_pago = ? LIMIT 1", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, conceptId);
                    setDecimal(statement, 3, amount);
                    setDate(statement, 4, date);
                    setEnum(statement, 5, method);
                }, JdbcPaymentDao::map);
    }

    @Override
    public List<Payment> findByStudent(Connection connection, int studentId, Integer academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE pg.id_estudiante = ? "
                + "AND (? IS NULL OR COALESCE(pg.año_academico, YEAR(pg.fecha_pago)) = ?)" + ORDER, statement -> {
                    statement.setInt(1, studentId);
                    setInteger(statement, 2, academicYear);
                    setInteger(statement, 3, academicYear);
                }, JdbcPaymentDao::map);
    }

    @Override
    public List<Payment> findByGuardian(Connection connection, int guardianId) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE pg.id_acudiente = ? OR pg.id_estudiante IN "
                + "(SELECT ea.id_estudiante FROM estudiante_acudiente ea WHERE ea.id_acudiente = ?)" + ORDER, statement -> {
                    statement.setInt(1, guardianId);
                    statement.setInt(2, guardianId);
                }, JdbcPaymentDao::map);
    }

    @Override
    public List<Payment> findByYear(Connection connection, int year) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT
                        + "WHERE COALESCE(pg.año_academico, YEAR(pg.fecha_pago)) = ?" + ORDER,
                statement -> statement.setInt(1, year), JdbcPaymentDao::map);
    }

    @Override
    public List<BillableStudent> findBillableStudents(Connection connection, int academicYear) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_BILLABLE, statement -> statement.setInt(1, academicYear),
                resultSet -> new BillableStudent(
                        resultSet.getInt("id_estudiante"),
                        resultSet.getString("numero_documento"),
                        resultSet.getString("nombre_estudiante"),
                        resultSet.getString("nombre_grado"),
                        getDate(resultSet, "fecha_matricula"),
                        resultSet.getString("acudiente"),
                        resultSet.getString("telefono")));
    }

    private static Payment map(ResultSet resultSet) throws SQLException {
        return new Payment(
                resultSet.getInt("id_pago"),
                resultSet.getInt("id_estudiante"),
                resultSet.getString("nombre_estudiante"),
                getInteger(resultSet, "id_acudiente"),
                resultSet.getString("nombre_acudiente"),
                resultSet.getInt("id_concepto"),
                resultSet.getString("codigo_concepto"),
                resultSet.getString("nombre_concepto"),
                resultSet.getBigDecimal("monto_pagado"),
                getDate(resultSet, "fecha_pago"),
                PaymentMethod.fromDbValue(resultSet.getString("metodo_pago")),
                resultSet.getString("numero_recibo"),
                getInteger(resultSet, "año_academico"),
                PaymentPeriod.fromDbValue(resultSet.getString("periodo_academico")),
                resultSet.getString("notas"));
    }
}
