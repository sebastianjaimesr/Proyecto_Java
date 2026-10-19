package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;
import com.littlehorizons.erp.model.resources.LoanStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link LoanDao}.
 *
 * <p>El SQL de cada tabla se construye una sola vez a partir de las constantes de
 * {@link BorrowerType}; los valores del usuario siempre se pasan como parámetros.</p>
 */
public class JdbcLoanDao implements LoanDao {

    private final Map<BorrowerType, Sql> sql = new EnumMap<>(BorrowerType.class);

    public JdbcLoanDao() {
        sql.put(BorrowerType.STUDENT, new Sql(BorrowerType.STUDENT, "estudiantes", "e"));
        sql.put(BorrowerType.TEACHER, new Sql(BorrowerType.TEACHER, "profesores", "e"));
    }

    /** Sentencias SQL de una tabla de préstamos. */
    private static final class Sql {
        final String select;
        final String insert;
        final String markReturned;
        final String markOverdue;

        Sql(BorrowerType type, String personTable, String alias) {
            String table = type.table();
            String borrower = type.borrowerColumn();
            this.select = "SELECT p.id_prestamo, p.id_recurso, r.codigo_recurso, r.nombre_recurso, p." + borrower
                    + " AS id_solicitante, CONCAT_WS(' ', " + alias + ".primer_nombre, " + alias + ".primer_apellido, "
                    + alias + ".segundo_apellido) AS nombre_solicitante, p.fecha_prestamo, p.fecha_devolucion_esperada, "
                    + "p.fecha_devolucion_real, p.estado_prestamo, p.observaciones FROM " + table + " p "
                    + "JOIN recursos r ON r.id_recurso = p.id_recurso "
                    + "JOIN " + personTable + " " + alias + " ON " + alias + "." + borrower + " = p." + borrower + " ";
            this.insert = "INSERT INTO " + table + " (id_recurso, " + borrower + ", fecha_prestamo, "
                    + "fecha_devolucion_esperada, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
            this.markReturned = "UPDATE " + table + " SET estado_prestamo = 'Devuelto', fecha_devolucion_real = ?, "
                    + "observaciones = ? WHERE id_prestamo = ?";
            this.markOverdue = "UPDATE " + table + " SET estado_prestamo = 'Vencido' "
                    + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?";
        }
    }

    @Override
    public int insert(Connection connection, Loan loan) throws SQLException {
        return JdbcSupport.insert(connection, sql.get(loan.borrowerType()).insert, statement -> {
            statement.setInt(1, loan.resourceId());
            statement.setInt(2, loan.borrowerId());
            setDate(statement, 3, loan.loanDate());
            setDate(statement, 4, loan.dueDate());
            setEnum(statement, 5, loan.status());
            setString(statement, 6, loan.notes());
        });
    }

    @Override
    public Optional<Loan> findById(Connection connection, BorrowerType type, int loanId) throws SQLException {
        return JdbcSupport.queryOne(connection, sql.get(type).select + "WHERE p.id_prestamo = ?",
                statement -> statement.setInt(1, loanId), resultSet -> map(resultSet, type));
    }

    @Override
    public List<Loan> findOpenByResource(Connection connection, int resourceId) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        for (BorrowerType type : BorrowerType.values()) {
            loans.addAll(JdbcSupport.queryList(connection, sql.get(type).select
                            + "WHERE p.id_recurso = ? AND p.estado_prestamo <> 'Devuelto'",
                    statement -> statement.setInt(1, resourceId), resultSet -> map(resultSet, type)));
        }
        return loans;
    }

    @Override
    public void markReturned(Connection connection, BorrowerType type, int loanId, LocalDate returnDate, String notes)
            throws SQLException {
        JdbcSupport.update(connection, sql.get(type).markReturned, statement -> {
            setDate(statement, 1, returnDate);
            setString(statement, 2, notes);
            statement.setInt(3, loanId);
        });
    }

    @Override
    public int markOverdue(Connection connection, LocalDate today) throws SQLException {
        int updated = 0;
        for (BorrowerType type : BorrowerType.values()) {
            updated += JdbcSupport.update(connection, sql.get(type).markOverdue, statement -> setDate(statement, 1, today));
        }
        return updated;
    }

    @Override
    public List<Loan> findOpen(Connection connection) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        for (BorrowerType type : BorrowerType.values()) {
            loans.addAll(JdbcSupport.queryList(connection, sql.get(type).select + "WHERE p.estado_prestamo <> 'Devuelto'",
                    JdbcSupport.NO_PARAMETERS, resultSet -> map(resultSet, type)));
        }
        loans.sort(Comparator.comparing(Loan::dueDate));
        return loans;
    }

    @Override
    public List<Loan> findByBorrower(Connection connection, BorrowerType type, int borrowerId) throws SQLException {
        return JdbcSupport.queryList(connection, sql.get(type).select + "WHERE p." + type.borrowerColumn()
                        + " = ? ORDER BY p.fecha_prestamo DESC, p.id_prestamo DESC",
                statement -> statement.setInt(1, borrowerId), resultSet -> map(resultSet, type));
    }

    private static Loan map(ResultSet resultSet, BorrowerType type) throws SQLException {
        return new Loan(
                resultSet.getInt("id_prestamo"),
                type,
                resultSet.getInt("id_recurso"),
                resultSet.getString("codigo_recurso"),
                resultSet.getString("nombre_recurso"),
                resultSet.getInt("id_solicitante"),
                resultSet.getString("nombre_solicitante"),
                getDate(resultSet, "fecha_prestamo"),
                getDate(resultSet, "fecha_devolucion_esperada"),
                getDate(resultSet, "fecha_devolucion_real"),
                LoanStatus.fromDbValue(resultSet.getString("estado_prestamo")),
                resultSet.getString("observaciones"));
    }
}
