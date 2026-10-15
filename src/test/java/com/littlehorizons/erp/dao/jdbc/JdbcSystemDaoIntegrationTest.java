package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.model.system.DatabaseInfo;
import com.littlehorizons.erp.service.SystemService;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.DatabaseConnection;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs against the real MySQL database configured in config.properties.
 * It is skipped (not failed) when that file does not exist, for example on a
 * fresh clone before the database is configured.
 */
class JdbcSystemDaoIntegrationTest {

    @Test
    void readsServerVersionAndOfficialTables() {
        Path configFile = AppConfig.resolveConfigPath();
        assumeTrue(Files.isRegularFile(configFile), "config.properties not found: integration test skipped");

        AppConfig config = AppConfig.load(configFile);
        DatabaseInfo info = new JdbcSystemDao(DatabaseConnection.fromConfig(config)).findDatabaseInfo();

        assertTrue(info.serverVersion().startsWith("8.") || info.serverVersion().startsWith("9."),
                "MySQL 8.0 or later is required, found " + info.serverVersion());
        assertEquals(config.getRequired("db.name").toLowerCase(Locale.ROOT),
                info.databaseName().toLowerCase(Locale.ROOT));
        assertTrue(info.tableNames().stream()
                        .map(name -> name.toLowerCase(Locale.ROOT))
                        .toList()
                        .containsAll(SystemService.OFFICIAL_TABLES),
                "the database must contain the 22 official tables");
    }
}
