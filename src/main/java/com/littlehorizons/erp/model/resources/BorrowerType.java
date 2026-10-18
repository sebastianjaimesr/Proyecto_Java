package com.littlehorizons.erp.model.resources;

/**
 * Who borrows a resource. The official schema keeps student and teacher loans
 * in separate tables, so each type maps to its own table and borrower column.
 * The names below are constants of the application, never user input.
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
