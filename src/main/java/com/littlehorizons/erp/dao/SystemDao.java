package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.system.DatabaseInfo;

/**
 * Read-only queries about the database server and its structure.
 */
public interface SystemDao {

    /**
     * Opens a connection and reads the server version, the selected database
     * and its base tables.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException if the
     *         database cannot be reached or queried
     */
    DatabaseInfo findDatabaseInfo();
}
