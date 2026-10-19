package com.littlehorizons.erp.model.resources;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Préstamo de un recurso a un estudiante o a un profesor ({@code prestamos_estudiante}
 * o {@code prestamos_profesor}).
 *
 * @param id           {@code null} antes de insertar el registro
 * @param returnDate   {@code null} mientras el recurso no se haya devuelto
 * @param borrowerName obtenido con join, para mostrar
 */
public record Loan(
        Integer id,
        BorrowerType borrowerType,
        int resourceId,
        String resourceCode,
        String resourceName,
        int borrowerId,
        String borrowerName,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status,
        String notes) {

    /** No devuelto y con la fecha límite vencida, diga lo que diga el estado guardado. */
    public boolean isOverdueOn(LocalDate today) {
        return returnDate == null && status != LoanStatus.RETURNED && dueDate.isBefore(today);
    }

    public long daysOverdue(LocalDate today) {
        return isOverdueOn(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }
}
