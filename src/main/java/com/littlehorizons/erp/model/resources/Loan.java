package com.littlehorizons.erp.model.resources;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Loan of a resource to a student or a teacher ({@code prestamos_estudiante}
 * or {@code prestamos_profesor}).
 *
 * @param id           {@code null} before the record is inserted
 * @param returnDate   {@code null} while the resource has not been returned
 * @param borrowerName joined, for display
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

    /** Not returned and past its due date, whatever the stored status says. */
    public boolean isOverdueOn(LocalDate today) {
        return returnDate == null && status != LoanStatus.RETURNED && dueDate.isBefore(today);
    }

    public long daysOverdue(LocalDate today) {
        return isOverdueOn(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }
}
