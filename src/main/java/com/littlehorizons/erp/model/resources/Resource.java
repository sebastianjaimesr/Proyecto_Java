package com.littlehorizons.erp.model.resources;

import java.time.LocalDate;

/**
 * Elemento del inventario que se puede prestar ({@code recursos}).
 *
 * @param id           {@code null} antes de insertar el registro
 * @param categoryName obtenido con join, para mostrar
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

    /** Copia del recurso con otro estado. */
    public Resource withStatus(ResourceStatus newStatus) {
        return new Resource(id, code, name, categoryId, categoryName, description, acquisitionDate, location, newStatus);
    }
}
