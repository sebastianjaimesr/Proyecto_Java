package com.littlehorizons.erp.service;

import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.controller.SystemController;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.model.system.DatabaseInfo;
import com.littlehorizons.erp.model.system.SchemaStatus;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses in-memory {@link com.littlehorizons.erp.dao.SystemDao} implementations,
 * possible because the service depends on the interface and not on JDBC.
 */
class SystemServiceTest {

    private static final ConfigurationSummary SUMMARY = new ConfigurationSummary(
            "config.properties", "jdbc:mysql://localhost:3306/erp_little_horizons",
            "erp_user", "logs/error_log.txt", "UTF-8", "17");

    @Test
    void completeSchemaIsDetectedIgnoringCase() {
        List<String> upperCaseTables = SystemService.OFFICIAL_TABLES.stream()
                .map(name -> name.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
        SystemService service = new SystemService(
                () -> new DatabaseInfo("8.0.46", "erp_little_horizons", upperCaseTables), SUMMARY);

        SchemaStatus status = service.checkSchema();

        assertTrue(status.isComplete());
        assertEquals(22, status.expectedTables());
        assertEquals(22, status.foundOfficialTables());
    }

    @Test
    void missingTablesAreListed() {
        List<String> tables = new ArrayList<>(SystemService.OFFICIAL_TABLES);
        tables.remove("prestamos_profesor");
        tables.remove("facturas");
        tables.add("tabla_extra");
        SystemService service = new SystemService(
                () -> new DatabaseInfo("8.0.46", "erp_little_horizons", tables), SUMMARY);

        SchemaStatus status = service.checkSchema();

        assertFalse(status.isComplete());
        assertEquals(List.of("facturas", "prestamos_profesor"), status.missingTables());
        assertEquals(20, status.foundOfficialTables());
    }

    @Test
    void controllerHidesTechnicalErrorsFromTheUser() {
        SystemService service = new SystemService(() -> {
            throw new DataAccessException("Could not open a connection to jdbc:mysql://x",
                    new SQLException("Communications link failure", "08S01"));
        }, SUMMARY);

        OperationResult<SchemaStatus> result = new SystemController(service).checkDatabase();

        assertFalse(result.success());
        assertNull(result.data());
        assertTrue(result.message().startsWith("No fue posible comunicarse con la base de datos"));
        assertFalse(result.message().contains("jdbc:"), "technical details stay in the log");
    }

    @Test
    void controllerReportsSuccessMessage() {
        SystemService service = new SystemService(
                () -> new DatabaseInfo("8.0.46", "erp_little_horizons", SystemService.OFFICIAL_TABLES), SUMMARY);

        OperationResult<SchemaStatus> result = new SystemController(service).checkDatabase();

        assertTrue(result.success());
        assertTrue(result.message().contains("22 tablas"));
    }
}
