package com.littlehorizons.erp.model.system;

import java.util.List;

/**
 * Resultado de comparar la base conectada con el esquema oficial.
 *
 * @param databaseInfo   datos leídos del servidor
 * @param expectedTables número de tablas definidas por el script oficial
 * @param missingTables  tablas oficiales que no existen en la base
 */
public record SchemaStatus(DatabaseInfo databaseInfo, int expectedTables, List<String> missingTables) {

    public SchemaStatus {
        missingTables = List.copyOf(missingTables);
    }

    /** Indica si la base de datos tiene todas las tablas oficiales. */
    public boolean isComplete() {
        return missingTables.isEmpty();
    }

    /** Cantidad de tablas oficiales que sí se encontraron. */
    public int foundOfficialTables() {
        return expectedTables - missingTables.size();
    }
}
