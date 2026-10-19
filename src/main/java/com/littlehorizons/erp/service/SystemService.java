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
 * Revisa la conexión a la base de datos y compara la base conectada con el
 * esquema oficial entregado por el instructor (22 tablas).
 */
public class SystemService {

    /** Tablas que crea el script oficial, en orden de creación. */
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
     * Se conecta a la base de datos y lista las tablas oficiales que faltan.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException si no se
     *         puede conectar a la base de datos
     */
    public SchemaStatus checkSchema() {
        DatabaseInfo info = systemDao.findDatabaseInfo();
        // MySQL en Windows guarda los nombres de tabla en minúsculas, así que se comparan sin distinguir mayúsculas.
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
