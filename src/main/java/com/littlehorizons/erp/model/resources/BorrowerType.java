package com.littlehorizons.erp.model.resources;

/**
 * Quién pide prestado un recurso. El esquema oficial guarda los préstamos de estudiantes y de profesores
 * en tablas separadas, así que cada tipo apunta a su propia tabla y columna de prestatario.
 * Los nombres de abajo son constantes de la aplicación, nunca entrada del usuario.
 */
public enum BorrowerType {

    STUDENT("Estudiante", "prestamos_estudiante", "id_estudiante"),
    TEACHER("Profesor", "prestamos_profesor", "id_profesor");

    private final String label;
    private final String table;
    private final String borrowerColumn;

    BorrowerType(String label, String table, String borrowerColumn) {
        this.label = label;
        this.table = table;
        this.borrowerColumn = borrowerColumn;
    }

    public String label() {
        return label;
    }

    public String table() {
        return table;
    }

    public String borrowerColumn() {
        return borrowerColumn;
    }
}
