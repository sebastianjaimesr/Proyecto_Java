package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.model.system.SchemaStatus;
import com.littlehorizons.erp.service.SystemService;

import java.util.Objects;

/**
 * Coordina el menú "Configuración y estado del sistema" con
 * {@link SystemService}.
 *
 * <p>Patrón de todos sus métodos: llaman al servicio correspondiente dentro de
 * {@code execute(...)} (heredado de {@link BaseController}), que devuelve un
 * {@link OperationResult} con el dato y un mensaje en español; si el servicio lanza
 * una excepción, la convierte en un mensaje de error para la vista.</p>
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
