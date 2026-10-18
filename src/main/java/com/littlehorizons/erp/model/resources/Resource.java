package com.littlehorizons.erp.model.resources;

import java.time.LocalDate;

/**
 * Inventory item that can be lent ({@code recursos}).
 *
 * @param id           {@code null} before the record is inserted
 * @param categoryName joined, for display
 */
public record Resource(
        Integer id,
        String code,
        String name,
        int categoryId,
        String categoryName,
        String description,
        LocalDate acquisitionDate,
        String location,
        ResourceStatus status) {

    public Resource withStatus(ResourceStatus newStatus) {
        return new Resource(id, code, name, categoryId, categoryName, description, acquisitionDate, location, newStatus);
    }
}
