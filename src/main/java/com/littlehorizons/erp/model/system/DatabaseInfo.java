package com.littlehorizons.erp.model.system;

import java.util.List;

/**
 * Facts read from the connected MySQL server.
 *
 * @param serverVersion value of {@code VERSION()}
 * @param databaseName  database selected by the connection
 * @param tableNames    base tables found in that database, in alphabetical order
 */
public record DatabaseInfo(String serverVersion, String databaseName, List<String> tableNames) {

    public DatabaseInfo {
        tableNames = List.copyOf(tableNames);
    }
}
