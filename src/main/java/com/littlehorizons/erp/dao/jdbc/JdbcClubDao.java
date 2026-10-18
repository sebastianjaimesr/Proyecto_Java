package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;
import com.littlehorizons.erp.model.extracurricular.MembershipStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * JDBC implementation of {@link ClubDao}.
 */
public class JdbcClubDao implements ClubDao {

    private static final String SELECT = "SELECT c.id_club, c.nombre_club, c.descripcion, c.id_profesor_supervisor, "
            + "CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS nombre_supervisor, c.capacidad_maxima, "
            + "c.horario_reunion, c.esta_activo, (SELECT COUNT(*) FROM membresias_club mc "
            + "WHERE mc.id_club = c.id_club AND mc.estado_membresia = 'Activa') AS miembros_activos "
            + "FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor ";
    private static final String SQL_INSERT = "INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, "
            + "capacidad_maxima, horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE clubes SET nombre_club = ?, descripcion = ?, "
            + "id_profesor_supervisor = ?, capacidad_maxima = ?, horario_reunion = ?, esta_activo = ? WHERE id_club = ?";
    private static final String SQL_LOCK = "SELECT id_club FROM clubes WHERE id_club = ? FOR UPDATE";
    private static final String SQL_COUNT_ACTIVE =
            "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'";

    private static final String MEMBERSHIP_SELECT = "SELECT mc.id_membresia, mc.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) AS nombre_estudiante, "
            + "mc.id_club, c.nombre_club, mc.fecha_ingreso, mc.estado_membresia, mc.fecha_retiro, mc.motivo_retiro "
            + "FROM membresias_club mc JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante "
            + "JOIN clubes c ON c.id_club = mc.id_club ";
    private static final String SQL_INSERT_MEMBERSHIP = "INSERT INTO membresias_club (id_estudiante, id_club, "
            + "fecha_ingreso, estado_membresia) VALUES (?, ?, ?, ?)";
    private static final String SQL_WITHDRAW = "UPDATE membresias_club SET estado_membresia = 'Retirada', "
            + "fecha_retiro = ?, motivo_retiro = ? WHERE id_membresia = ?";

    @Override
    public int insert(Connection connection, Club club) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, club));
    }

    @Override
    public void update(Connection connection, Club club) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, club);
            statement.setInt(7, club.id());
        });
    }

    @Override
    public Optional<Club> findById(Connection connection, int clubId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE c.id_club = ?",
                statement -> statement.setInt(1, clubId), JdbcClubDao::map);
    }

    @Override
    public Optional<Club> lockById(Connection connection, int clubId) throws SQLException {
        boolean exists = JdbcSupport.exists(connection, SQL_LOCK, statement -> statement.setInt(1, clubId));
        return exists ? findById(connection, clubId) : Optional.empty();
    }

    @Override
    public Optional<Club> findByName(Connection connection, String name) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE c.nombre_club = ?",
                statement -> statement.setString(1, name), JdbcClubDao::map);
    }

    @Override
    public List<Club> findAll(Connection connection, boolean onlyActive) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE ? = FALSE OR c.esta_activo = TRUE ORDER BY c.nombre_club",
                statement -> statement.setBoolean(1, onlyActive), JdbcClubDao::map);
    }

    @Override
    public int countActiveMembers(Connection connection, int clubId) throws SQLException {
        return JdbcSupport.queryInt(connection, SQL_COUNT_ACTIVE, statement -> statement.setInt(1, clubId));
    }

    @Override
    public int insertMembership(Connection connection, ClubMembership membership) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT_MEMBERSHIP, statement -> {
            statement.setInt(1, membership.studentId());
            statement.setInt(2, membership.clubId());
            setDate(statement, 3, membership.joinDate());
            setEnum(statement, 4, membership.status());
        });
    }

    @Override
    public Optional<ClubMembership> findMembershipById(Connection connection, int membershipId) throws SQLException {
        return JdbcSupport.queryOne(connection, MEMBERSHIP_SELECT + "WHERE mc.id_membresia = ?",
                statement -> statement.setInt(1, membershipId), JdbcClubDao::mapMembership);
    }

    @Override
    public Optional<ClubMembership> findActiveMembership(Connection connection, int studentId, int clubId)
            throws SQLException {
        return JdbcSupport.queryOne(connection, MEMBERSHIP_SELECT
                + "WHERE mc.id_estudiante = ? AND mc.id_club = ? AND mc.estado_membresia = 'Activa'", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, clubId);
                }, JdbcClubDao::mapMembership);
    }

    @Override
    public void withdrawMembership(Connection connection, int membershipId, LocalDate date, String reason)
            throws SQLException {
        JdbcSupport.update(connection, SQL_WITHDRAW, statement -> {
            setDate(statement, 1, date);
            setString(statement, 2, reason);
            statement.setInt(3, membershipId);
        });
    }

    @Override
    public List<ClubMembership> findMembers(Connection connection, int clubId, boolean onlyActive) throws SQLException {
        return JdbcSupport.queryList(connection, MEMBERSHIP_SELECT
                + "WHERE mc.id_club = ? AND (? = FALSE OR mc.estado_membresia = 'Activa') "
                + "ORDER BY mc.estado_membresia, nombre_estudiante", statement -> {
                    statement.setInt(1, clubId);
                    statement.setBoolean(2, onlyActive);
                }, JdbcClubDao::mapMembership);
    }

    @Override
    public List<ClubMembership> findByStudent(Connection connection, int studentId) throws SQLException {
        return JdbcSupport.queryList(connection, MEMBERSHIP_SELECT
                        + "WHERE mc.id_estudiante = ? ORDER BY mc.fecha_ingreso DESC",
                statement -> statement.setInt(1, studentId), JdbcClubDao::mapMembership);
    }

    private static void bindFields(PreparedStatement statement, Club club) throws SQLException {
        statement.setString(1, club.name());
        setString(statement, 2, club.description());
        setInteger(statement, 3, club.supervisorId());
        statement.setInt(4, club.capacity());
        setString(statement, 5, club.meetingSchedule());
        statement.setBoolean(6, club.active());
    }

    private static Club map(ResultSet resultSet) throws SQLException {
        Integer capacity = getInteger(resultSet, "capacidad_maxima");
        return new Club(
                resultSet.getInt("id_club"),
                resultSet.getString("nombre_club"),
                resultSet.getString("descripcion"),
                getInteger(resultSet, "id_profesor_supervisor"),
                resultSet.getString("nombre_supervisor"),
                capacity == null ? Club.DEFAULT_CAPACITY : capacity,
                resultSet.getString("horario_reunion"),
                resultSet.getBoolean("esta_activo"),
                resultSet.getInt("miembros_activos"));
    }

    private static ClubMembership mapMembership(ResultSet resultSet) throws SQLException {
        return new ClubMembership(
                resultSet.getInt("id_membresia"),
                resultSet.getInt("id_estudiante"),
                resultSet.getString("nombre_estudiante"),
                resultSet.getInt("id_club"),
                resultSet.getString("nombre_club"),
                getDate(resultSet, "fecha_ingreso"),
                MembershipStatus.fromDbValue(resultSet.getString("estado_membresia")),
                getDate(resultSet, "fecha_retiro"),
                resultSet.getString("motivo_retiro"));
    }
}
