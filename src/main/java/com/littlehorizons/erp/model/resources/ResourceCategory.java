package com.littlehorizons.erp.model.resources;

/**
 * Resource category ({@code categorias_recurso}); the official script loads
 * Libro, Equipo de Laboratorio, Equipo Deportivo, Dispositivo Tecnológico and
 * Material Didáctico.
 */
public record ResourceCategory(int id, String name, String description) {
}
