package com.littlehorizons.erp.support;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcTransactionManager;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.ConnectionProvider;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Crea una base desechable {@code erp_little_horizons_test} a partir de las secciones DDL
 * de {@code docs/database.sql} (script oficial más cambios autorizados,
 * sin datos de prueba), así las pruebas de integración nunca tocan los datos reales de
 * {@code erp_little_horizons}.
 *
 * <p>Las pruebas de integración se omiten cuando falta {@code config.properties}.</p>
 */
public final class TestDatabase {

    public static final String TEST_DATABASE = "erp_little_horizons_test";
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    public static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private static final String DDL_END_MARKER = "-- SECCIÓN 2. DML";

    private final ConnectionProvider connectionProvider;

    private TestDatabase(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Borra y vuelve a crear la base de pruebas.
     *
     * @throws org.opentest4j.TestAbortedException cuando no hay configuración local
     */
    public static TestDatabase recreate() {
        Path configFile = AppConfig.resolveConfigPath();
        assumeTrue(Files.isRegularFile(configFile), "config.properties not found: integration test skipped");
        AppConfig realConfig = AppConfig.load(configFile);

        Properties properties = new Properties();
        for (String key : List.of("db.host", "db.port", "db.user", "db.password", "db.params")) {
            properties.setProperty(key, realConfig.getOptional(key, ""));
        }
        properties.setProperty("db.name", "mysql");
        ConnectionProvider serverConnection = DatabaseConnection.fromConfig(AppConfig.fromProperties(properties));
        runScript(serverConnection, ddlStatements());

        properties.setProperty("db.name", TEST_DATABASE);
        return new TestDatabase(DatabaseConnection.fromConfig(AppConfig.fromProperties(properties)));
    }

    public ConnectionProvider connectionProvider() {
        return connectionProvider;
    }

    public TransactionManager transactionManager() {
        return new JdbcTransactionManager(connectionProvider);
    }

    /** Ejecuta sentencias SQL en la base de pruebas (datos de prueba). */
    public void execute(String... statements) {
        runScript(connectionProvider, Arrays.asList(statements));
    }

    private static List<String> ddlStatements() {
        Path script = Paths.get("docs", "database.sql");
        String content;
        try {
            content = Files.readString(script, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + script.toAbsolutePath(), e);
        }
        int end = content.indexOf(DDL_END_MARKER);
        String ddl = (end >= 0 ? content.substring(0, end) : content)
                .replace("erp_little_horizons", TEST_DATABASE);
        String withoutComments = ddl.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(withoutComments.split(";"))
                .map(String::strip)
                .filter(statement -> !statement.isEmpty())
                .collect(Collectors.toList());
    }

    private static void runScript(ConnectionProvider provider, List<String> statements) {
        try (Connection connection = provider.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not prepare the test database: " + e.getMessage(), e);
        }
    }
}
