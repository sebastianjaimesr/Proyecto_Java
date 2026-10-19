package com.littlehorizons.erp.model.resources;

/**
 * Categoría de recurso ({@code categorias_recurso}); el script oficial carga
 * Libro, Equipo de Laboratorio, Equipo Deportivo, Dispositivo Tecnológico y
 * Material Didáctico.
 */
public record ResourceCategory(int id, String name, String description) {
}
