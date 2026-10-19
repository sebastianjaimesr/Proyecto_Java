package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.model.system.SchemaStatus;
import com.littlehorizons.erp.service.SystemService;

import java.util.Objects;

/**
 * Coordina el menú "Configuración y estado del sistema" con
 * {@link SystemService}.
 */
public class SystemController extends BaseController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = Objects.requireNonNull(systemService, "systemService");
    }

    public OperationResult<SchemaStatus> checkDatabase() {
        return execute(systemService::checkSchema, status -> status.isComplete()
                ? "Conexión exitosa. La base de datos tiene las " + status.expectedTables()
                        + " tablas del esquema oficial."
                : "Conexión exitosa, pero faltan " + status.missingTables().size()
                        + " tablas del esquema oficial.");
    }

    public OperationResult<ConfigurationSummary> getConfiguration() {
        return execute(systemService::getConfigurationSummary, "Configuración cargada.");
    }
}
