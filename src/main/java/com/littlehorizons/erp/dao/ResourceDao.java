package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceCategory;
import com.littlehorizons.erp.model.resources.ResourceStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code recursos} y del catálogo de solo lectura {@code categorias_recurso}.
 */
public interface ResourceDao {

    List<ResourceCategory> findCategories(Connection connection) throws SQLException;

    int insert(Connection connection, Resource resource) throws SQLException;

    void update(Connection connection, Resource resource) throws SQLException;

    void updateStatus(Connection connection, int resourceId, ResourceStatus status) throws SQLException;

    Optional<Resource> findById(Connection connection, int resourceId) throws SQLException;

    /** Lee el recurso con {@code SELECT ... FOR UPDATE} para poner en fila préstamos y devoluciones. */
    Optional<Resource> lockById(Connection connection, int resourceId) throws SQLException;

    Optional<Resource> findByCode(Connection connection, String code) throws SQLException;

    /**
     * Recursos filtrados por texto (código o nombre), categoría y estado; los filtros
     * null se ignoran.
     */
    List<Resource> search(Connection connection, String text, Integer categoryId, ResourceStatus status, int limit)
            throws SQLException;
}
