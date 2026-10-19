package com.littlehorizons.erp.model.system;

import java.util.List;

/**
 * Datos leídos del servidor MySQL conectado.
 *
 * @param serverVersion valor de {@code VERSION()}
 * @param databaseName  base de datos seleccionada por la conexión
 * @param tableNames    tablas encontradas en esa base, en orden alfabético
 */
public record DatabaseInfo(String serverVersion, String databaseName, List<String> tableNames) {

    public DatabaseInfo {
        tableNames = List.copyOf(tableNames);
    }
}
