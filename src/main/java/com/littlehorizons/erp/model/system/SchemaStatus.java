package com.littlehorizons.erp.model.system;

import java.util.List;

/**
 * Result of comparing the connected database with the official schema.
 *
 * @param databaseInfo   facts read from the server
 * @param expectedTables number of tables defined by the official script
 * @param missingTables  official tables that do not exist in the database
 */
public record SchemaStatus(DatabaseInfo databaseInfo, int expectedTables, List<String> missingTables) {

    public SchemaStatus {
        missingTables = List.copyOf(missingTables);
    }

    public boolean isComplete() {
        return missingTables.isEmpty();
    }

    public int foundOfficialTables() {
        return expectedTables - missingTables.size();
    }
}
