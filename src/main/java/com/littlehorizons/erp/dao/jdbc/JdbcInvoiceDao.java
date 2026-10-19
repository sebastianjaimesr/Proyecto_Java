package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.model.finance.Invoice;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getTimestamp;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setTimestamp;

/**
 * Implementación JDBC de {@link InvoiceDao}.
 */
public class JdbcInvoiceDao implements InvoiceDao {

    private static final String SELECT = "SELECT id_factura, numero_factura, id_pago, fecha_emision, fecha_creacion, "
            + "cufe, nombre_archivo_pdf, ruta_archivo FROM facturas ";
    private static final String SQL_INSERT = "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, "
            + "fecha_creacion) VALUES (?, ?, ?, ?)";
    private static final String SQL_NUMBER = "UPDATE facturas SET numero_factura = ?, cufe = ? WHERE id_factura = ?";
    private static final String SQL_PDF = "UPDATE facturas SET nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?";

    @Override
    public int insertDraft(Connection connection, int paymentId, LocalDate issueDate, LocalDateTime issuedAt)
            throws SQLException {
        String temporaryNumber = "TMP-" + UUID.randomUUID();
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setString(1, temporaryNumber);
            statement.setInt(2, paymentId);
            setDate(statement, 3, issueDate);
            setTimestamp(statement, 4, issuedAt);
        });
    }

    @Override
    public void assignNumberAndCufe(Connection connection, int invoiceId, String number, String cufe)
            throws SQLException {
        JdbcSupport.update(connection, SQL_NUMBER, statement -> {
            statement.setString(1, number);
            statement.setString(2, cufe);
            statement.setInt(3, invoiceId);
        });
    }

    @Override
    public void setPdfFile(Connection connection, int invoiceId, String fileName, String path) throws SQLException {
        JdbcSupport.update(connection, SQL_PDF, statement -> {
            statement.setString(1, fileName);
            statement.setString(2, path);
            statement.setInt(3, invoiceId);
        });
    }

    @Override
    public Optional<Invoice> findById(Connection connection, int invoiceId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE id_factura = ?",
                statement -> statement.setInt(1, invoiceId), JdbcInvoiceDao::map);
    }

    @Override
    public Optional<Invoice> findByPayment(Connection connection, int paymentId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE id_pago = ? ORDER BY id_factura LIMIT 1",
                statement -> statement.setInt(1, paymentId), JdbcInvoiceDao::map);
    }

    @Override
    public List<Invoice> findWithoutPdf(Connection connection) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE ruta_archivo IS NULL ORDER BY id_factura",
                JdbcSupport.NO_PARAMETERS, JdbcInvoiceDao::map);
    }

    /** Convierte una fila de {@code facturas} en un objeto {@link Invoice}. */
    private static Invoice map(ResultSet resultSet) throws SQLException {
        return new Invoice(
                resultSet.getInt("id_factura"),
                resultSet.getString("numero_factura"),
                resultSet.getInt("id_pago"),
                getDate(resultSet, "fecha_emision"),
                getTimestamp(resultSet, "fecha_creacion"),
                resultSet.getString("cufe"),
                resultSet.getString("nombre_archivo_pdf"),
                resultSet.getString("ruta_archivo"));
    }
}
