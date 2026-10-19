package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;
import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceCategory;
import com.littlehorizons.erp.model.resources.ResourceStatus;
import com.littlehorizons.erp.service.LoanService;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pantallas de "6. Gestión de recursos".
 */
public class ResourceMenuView {

    private static final List<String> RESOURCE_HEADERS =
            List.of("Id", "Código", "Recurso", "Categoría", "Ubicación", "Estado");

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final ResourceController controller;
    private final StudentMenuView studentView;
    private final TeacherMenuView teacherView;

    public ResourceMenuView(ViewSupport support, ResourceController controller, StudentMenuView studentView,
                            TeacherMenuView teacherView) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.controller = Objects.requireNonNull(controller, "controller");
        this.studentView = Objects.requireNonNull(studentView, "studentView");
        this.teacherView = Objects.requireNonNull(teacherView, "teacherView");
    }

    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Registrar recurso", this::register),
                        new Menu.Item("Consultar inventario", this::list),
                        new Menu.Item("Actualizar recurso o cambiar su estado", this::update),
                        new Menu.Item("Prestar recurso a un estudiante", () -> lend(BorrowerType.STUDENT)),
                        new Menu.Item("Prestar recurso a un profesor", () -> lend(BorrowerType.TEACHER)),
                        new Menu.Item("Registrar devolución", this::returnLoan),
                        new Menu.Item("Préstamos sin devolver", this::openLoans),
                        new Menu.Item("Reporte de préstamos vencidos", this::overdue),
                        new Menu.Item("Historial de préstamos de un estudiante o profesor", this::history)),
                "GESTIÓN DE RECURSOS").run();
    }

    private Optional<ResourceCategory> pickCategory() {
        Optional<List<ResourceCategory>> categories = support.quiet(controller.findCategories());
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        console.println("Categoría:");
        return Optional.of(console.readChoice("Opción: ", categories.get(), ResourceCategory::name, "categoría"));
    }

    private Optional<Resource> pickResource(ResourceStatus status) {
        String text = console.readLine("Buscar recurso por código o nombre (Enter para listar): ");
        Optional<List<Resource>> resources = support.quiet(controller.searchResources(text, null, status));
        if (resources.isEmpty() || resources.get().isEmpty()) {
            console.printWarning("No se encontraron recursos" + (status == null ? "." : " en estado " + status.dbValue() + "."));
            return Optional.empty();
        }
        printResources(resources.get());
        int id = console.readId("Id del recurso: ", "id");
        return resources.get().stream().filter(resource -> resource.id() == id).findFirst()
                .or(() -> {
                    console.printWarning("El id " + id + " no está en la lista.");
                    return Optional.empty();
                });
    }

    private void printResources(List<Resource> resources) {
        console.printTable(RESOURCE_HEADERS, resources.stream().map(resource -> List.of(String.valueOf(resource.id()),
                resource.code(), resource.name(), resource.categoryName(), Formats.text(resource.location()),
                resource.status().dbValue())).collect(Collectors.toList()));
    }

    private void register() {
        console.printTitle("Registrar recurso");
        Optional<ResourceCategory> category = pickCategory();
        if (category.isEmpty()) {
            support.notFound();
            return;
        }
        String code = console.readCode("Código de inventario (ej: LIB-0001): ", "código", 50);
        String name = console.readRequiredText("Nombre: ", "nombre", 150);
        String description = support.readOptional("Descripción", null,
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        LocalDate acquired = console.confirm("¿Registrar fecha de adquisición?") ? support.readDate("Fecha de adquisición") : null;
        String location = support.readOptional("Ubicación física (Biblioteca, Laboratorio 1...)", null,
                raw -> InputValidator.requireText(raw, "ubicación", 100));
        support.show(controller.registerResource(new Resource(null, code, name, category.get().id(), category.get().name(),
                description, acquired, location, ResourceStatus.AVAILABLE)));
        console.pause();
    }

    private void list() {
        console.printTitle("Inventario");
        String text = console.readLine("Filtrar por código o nombre (Enter para todos): ");
        Integer categoryId = console.confirm("¿Filtrar por categoría?")
                ? pickCategory().map(ResourceCategory::id).orElse(null) : null;
        ResourceStatus status = console.confirm("¿Filtrar por estado?") ? support.readEnum("Estado", ResourceStatus.class) : null;
        support.show(controller.searchResources(text, categoryId, status)).ifPresent(this::printResources);
        support.quiet(controller.availability()).ifPresent(this::printAvailability);
        console.pause();
    }

    void printAvailability(Map<String, Map<ResourceStatus, Long>> availability) {
        console.println("Disponibilidad por categoría:");
        List<String> headers = new java.util.ArrayList<>(List.of("Categoría"));
        Arrays.stream(ResourceStatus.values()).map(ResourceStatus::dbValue).forEach(headers::add);
        console.printTable(headers, availability.entrySet().stream().map(entry -> {
            List<String> row = new java.util.ArrayList<>(List.of(entry.getKey()));
            Arrays.stream(ResourceStatus.values())
                    .map(status -> String.valueOf(entry.getValue().getOrDefault(status, 0L)))
                    .forEach(row::add);
            return row;
        }).collect(Collectors.toList()));
    }

    private void update() {
        console.printTitle("Actualizar recurso");
        Optional<Resource> selected = pickResource(null);
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        Resource resource = selected.get();
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        String name = support.readTextOrKeep("Nombre", resource.name(), true);
        String description = support.readOptional("Descripción", resource.description(),
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        String location = support.readOptional("Ubicación", resource.location(),
                raw -> InputValidator.requireText(raw, "ubicación", 100));
        ResourceStatus status = resource.status();
        if (resource.status() == ResourceStatus.LOANED) {
            console.printInfo("El recurso está prestado: su estado cambiará al registrar la devolución.");
        } else if (console.confirm("¿Cambiar el estado (actual: " + resource.status().dbValue() + ")?")) {
            status = support.readEnum("Nuevo estado", List.of(ResourceStatus.AVAILABLE, ResourceStatus.MAINTENANCE,
                    ResourceStatus.RETIRED));
        }
        support.show(controller.updateResource(new Resource(resource.id(), resource.code(), name, resource.categoryId(),
                resource.categoryName(), description, resource.acquisitionDate(), location, status)));
        console.pause();
    }

    private void lend(BorrowerType type) {
        console.printTitle("Préstamo a " + type.label().toLowerCase(java.util.Locale.ROOT));
        Optional<Resource> resource = pickResource(ResourceStatus.AVAILABLE);
        if (resource.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Integer> borrowerId = type == BorrowerType.STUDENT
                ? studentView.pickStudent().map(Student::id)
                : teacherView.pickTeacher().map(Teacher::id);
        if (borrowerId.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate loanDate = support.readDateOrToday("Fecha de préstamo");
        LocalDate dueDate = console.readValid("Fecha programada de devolución (AAAA-MM-DD, máximo "
                + LoanService.MAX_LOAN_DAYS + " días) [" + loanDate.plusDays(8) + "]: ", raw -> raw.isBlank()
                ? loanDate.plusDays(8) : InputValidator.parseDate(raw, "fecha de devolución"));
        String notes = support.readOptional("Observaciones", null,
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        support.show(controller.lend(type, resource.get().id(), borrowerId.get(), loanDate, dueDate, notes));
        console.pause();
    }

    private void returnLoan() {
        console.printTitle("Registrar devolución");
        Optional<List<Loan>> open = support.quiet(controller.findOpen());
        if (open.isEmpty() || open.get().isEmpty()) {
            console.printWarning("No hay préstamos sin devolver.");
            console.pause();
            return;
        }
        console.println("Préstamo:");
        Loan loan = console.readChoice("Opción: ", open.get(), item -> item.resourceCode() + " " + item.resourceName()
                + " - " + item.borrowerType().label() + ": " + item.borrowerName() + " (vence " + item.dueDate() + ")",
                "préstamo");
        LocalDate date = support.readDateOrToday("Fecha real de devolución");
        String notes = support.readOptional("Observaciones de la devolución", loan.notes(),
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        support.show(controller.returnLoan(loan.borrowerType(), loan.id(), date, notes));
        console.pause();
    }

    private void openLoans() {
        console.printTitle("Préstamos sin devolver");
        support.show(controller.findOpen()).ifPresent(this::printLoans);
        console.pause();
    }

    private void overdue() {
        console.printTitle("Préstamos vencidos");
        support.show(controller.refreshOverdue());
        support.show(controller.findOverdue()).ifPresent(this::printLoans);
        console.pause();
    }

    void printLoans(List<Loan> loans) {
        console.printTable(List.of("Id", "Tipo", "Recurso", "Solicitante", "Préstamo", "Devolver", "Estado", "Días de mora"),
                loans.stream().map(loan -> List.of(String.valueOf(loan.id()), loan.borrowerType().label(),
                        loan.resourceCode() + " " + loan.resourceName(), loan.borrowerName(),
                        Formats.date(loan.loanDate()), Formats.date(loan.dueDate()), loan.status().dbValue(),
                        String.valueOf(controller.daysOverdue(loan)))).collect(Collectors.toList()));
    }

    private void history() {
        console.printTitle("Historial de préstamos");
        BorrowerType type = console.readChoice("Tipo de solicitante: ", List.of(BorrowerType.values()),
                BorrowerType::label, "tipo");
        Optional<Integer> borrowerId = type == BorrowerType.STUDENT
                ? studentView.pickStudent().map(Student::id)
                : teacherView.pickTeacher().map(Teacher::id);
        if (borrowerId.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(controller.findByBorrower(type, borrowerId.get())).ifPresent(loans -> console.printTable(
                List.of("Id", "Recurso", "Préstamo", "Devolver", "Devuelto", "Estado", "Observaciones"),
                loans.stream().map(loan -> List.of(String.valueOf(loan.id()), loan.resourceCode() + " " + loan.resourceName(),
                        Formats.date(loan.loanDate()), Formats.date(loan.dueDate()), Formats.date(loan.returnDate()),
                        loan.status().dbValue(), Formats.text(loan.notes()))).collect(Collectors.toList())));
        console.pause();
    }
}
