package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;
import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceCategory;
import com.littlehorizons.erp.model.resources.ResourceStatus;
import com.littlehorizons.erp.service.LoanService;
import com.littlehorizons.erp.service.ResourceService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordina "Gestión de recursos": inventario, préstamos y devoluciones.
 *
 * <p>Patrón de todos sus métodos: llaman al servicio correspondiente dentro de
 * {@code execute(...)} (heredado de {@link BaseController}), que devuelve un
 * {@link OperationResult} con el dato y un mensaje en español; si el servicio lanza
 * una excepción, la convierte en un mensaje de error para la vista.</p>
 */
public class ResourceController extends BaseController {

    private final ResourceService resourceService;
    private final LoanService loanService;

    public ResourceController(ResourceService resourceService, LoanService loanService) {
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService");
        this.loanService = Objects.requireNonNull(loanService, "loanService");
    }

    public OperationResult<List<ResourceCategory>> findCategories() {
        return execute(resourceService::findCategories, "Categorías cargadas.");
    }

    public OperationResult<Resource> registerResource(Resource draft) {
        return execute(() -> resourceService.register(draft),
                resource -> "Recurso " + resource.code() + " registrado con id " + resource.id() + ".");
    }

    public OperationResult<Resource> updateResource(Resource changes) {
        return execute(() -> resourceService.update(changes), "Recurso actualizado.");
    }

    public OperationResult<List<Resource>> searchResources(String text, Integer categoryId, ResourceStatus status) {
        return execute(() -> resourceService.search(text, categoryId, status), list -> list.size() + " recurso(s).");
    }

    public OperationResult<Map<String, Map<ResourceStatus, Long>>> availability() {
        return execute(resourceService::availabilityByCategory, "Disponibilidad calculada.");
    }

    public OperationResult<Loan> lend(BorrowerType type, int resourceId, int borrowerId, LocalDate loanDate,
                                      LocalDate dueDate, String notes) {
        return execute(() -> loanService.lend(type, resourceId, borrowerId, loanDate, dueDate, notes),
                loan -> "Préstamo " + loan.id() + " registrado: " + loan.resourceName() + " a " + loan.borrowerName()
                        + " hasta el " + loan.dueDate() + ".");
    }

    public OperationResult<Loan> returnLoan(BorrowerType type, int loanId, LocalDate returnDate, String notes) {
        return execute(() -> loanService.returnLoan(type, loanId, returnDate, notes),
                loan -> "Devolución registrada. " + loan.resourceName() + " quedó disponible.");
    }

    public OperationResult<Integer> refreshOverdue() {
        return execute(loanService::refreshOverdueStatus, count -> count + " préstamo(s) marcados como Vencido.");
    }

    public OperationResult<List<Loan>> findOverdue() {
        return execute(loanService::findOverdue, list -> list.size() + " préstamo(s) vencido(s).");
    }

    public OperationResult<List<Loan>> findOpen() {
        return execute(loanService::findOpen, list -> list.size() + " préstamo(s) sin devolver.");
    }

    public OperationResult<List<Loan>> findByBorrower(BorrowerType type, int borrowerId) {
        return execute(() -> loanService.findByBorrower(type, borrowerId), list -> list.size() + " préstamo(s).");
    }

    public long daysOverdue(Loan loan) {
        return loanService.daysOverdue(loan);
    }
}
