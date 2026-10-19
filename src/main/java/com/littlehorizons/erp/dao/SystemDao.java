package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.system.DatabaseInfo;

/**
 * Consultas de solo lectura sobre el servidor de base de datos y su estructura.
 */
public interface SystemDao {

    /**
     * Abre una conexión y lee la versión del servidor, la base seleccionada
     * y sus tablas.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException si no se
     *         puede conectar o consultar la base de datos
     */
    DatabaseInfo findDatabaseInfo();
}
