package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.SystemDao;
import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.model.system.DatabaseInfo;
import com.littlehorizons.erp.model.system.SchemaStatus;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks the database connection and compares the connected database with the
 * official schema provided by the instructor (22 tables).
 */
public class SystemService {

    /** Tables created by the official script, in creation order. */
    public static final List<String> OFFICIAL_TABLES = List.of(
            "grados", "estudiantes", "acudientes", "estudiante_acudiente", "matriculas",
            "materias", "profesores", "asignaciones_profesor", "inscripciones_materia",
            "registros_academicos", "asistencias", "clubes", "membresias_club", "eventos",
            "participantes_evento", "conceptos_pago", "pagos", "facturas",
            "categorias_recurso", "recursos", "prestamos_estudiante", "prestamos_profesor");

    private final SystemDao systemDao;
    private final ConfigurationSummary configurationSummary;

    public SystemService(SystemDao systemDao, ConfigurationSummary configurationSummary) {
        this.systemDao = Objects.requireNonNull(systemDao, "systemDao");
        this.configurationSummary = Objects.requireNonNull(configurationSummary, "configurationSummary");
    }

    /**
     * Connects to the database and lists the official tables that are missing.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException if the
     *         database cannot be reached
     */
    public SchemaStatus checkSchema() {
        DatabaseInfo info = systemDao.findDatabaseInfo();
        // MySQL on Windows stores table names in lower case, so compare ignoring case.
        Set<String> existing = info.tableNames().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> missing = OFFICIAL_TABLES.stream()
                .filter(table -> !existing.contains(table))
                .collect(Collectors.toList());
        return new SchemaStatus(info, OFFICIAL_TABLES.size(), missing);
    }

    public ConfigurationSummary getConfigurationSummary() {
        return configurationSummary;
    }
}
