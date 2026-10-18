package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceCategory;
import com.littlehorizons.erp.model.resources.ResourceStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code recursos} and the read-only {@code categorias_recurso}.
 */
public interface ResourceDao {

    List<ResourceCategory> findCategories(Connection connection) throws SQLException;

    int insert(Connection connection, Resource resource) throws SQLException;

    void update(Connection connection, Resource resource) throws SQLException;

    void updateStatus(Connection connection, int resourceId, ResourceStatus status) throws SQLException;

    Optional<Resource> findById(Connection connection, int resourceId) throws SQLException;

    /** Reads the resource with {@code SELECT ... FOR UPDATE} to serialize loans and returns. */
    Optional<Resource> lockById(Connection connection, int resourceId) throws SQLException;

    Optional<Resource> findByCode(Connection connection, String code) throws SQLException;

    /**
     * Resources filtered by text (code or name), category and status; null
     * filters are ignored.
     */
    List<Resource> search(Connection connection, String text, Integer categoryId, ResourceStatus status, int limit)
            throws SQLException;
}
