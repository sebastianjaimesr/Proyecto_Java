package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.ChargeStatus;
import com.littlehorizons.erp.model.finance.OverdueObligation;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PaymentMethod;
import com.littlehorizons.erp.model.finance.PaymentPeriod;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.service.InvoiceService;
import com.littlehorizons.erp.service.PaymentService;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Screens of "5. Gestión financiera".
 */
public class FinanceMenuView {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final FinanceController controller;
    private final StudentMenuView studentView;

    public FinanceMenuView(ViewSupport support, FinanceController controller, StudentMenuView studentView) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.controller = Objects.requireNonNull(controller, "controller");
        this.studentView = Objects.requireNonNull(studentView, "studentView");
    }

    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Conceptos de pago y valores por estudiante", this::showConceptsMenu),
                        new Menu.Item("Registrar pago y generar factura PDF", this::registerPayment),
                        new Menu.Item("Historial de pagos de un estudiante", this::historyByStudent),
                        new Menu.Item("Historial de pagos de un acudiente", this::historyByGuardian),
                        new Menu.Item("Estado de cuenta (pagos parciales y completos)", this::statement),
                        new Menu.Item("Obligaciones vencidas (más de un mes)", this::overdue),
                        new Menu.Item("Regenerar facturas PDF pendientes", this::regenerate)),
                "GESTIÓN FINANCIERA").run();
    }

    private void showConceptsMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Consultar catálogo de conceptos", this::listConcepts),
                        new Menu.Item("Registrar concepto", this::createConcept),
                        new Menu.Item("Actualizar concepto", this::updateConcept),
                        new Menu.Item("Registrar valor personalizado, beca o descuento por hermanos", this::addAdjustment),
                        new Menu.Item("Consultar o eliminar ajustes de un estudiante", this::listAdjustments)),
                "CONCEPTOS DE PAGO").run();
    }

    // -------------------------------------------------------------- concepts

    private Optional<PaymentConcept> pickConcept(boolean onlyActive) {
        Optional<List<PaymentConcept>> concepts = support.quiet(controller.findConcepts(onlyActive));
        if (concepts.isEmpty() || concepts.get().isEmpty()) {
            console.printWarning("No hay conceptos de pago.");
            return Optional.empty();
        }
        console.println("Concepto:");
        return Optional.of(console.readChoice("Opción: ", concepts.get(),
                concept -> concept.code() + " - " + concept.name() + " (" + Formats.money(concept.baseAmount()) + ")",
                "concepto"));
    }

    private void listConcepts() {
        console.printTitle("Catálogo de conceptos de pago");
        support.show(controller.findConcepts(false)).ifPresent(concepts -> console.printTable(
                List.of("Id", "Código", "Concepto", "Valor base", "Recurrente", "Activo"),
                concepts.stream().map(concept -> List.of(String.valueOf(concept.id()), concept.code(), concept.name(),
                        Formats.money(concept.baseAmount()), Formats.yesNo(concept.recurring()),
                        Formats.yesNo(concept.active()))).collect(Collectors.toList())));
        console.pause();
    }

    private void createConcept() {
        console.printTitle("Registrar concepto");
        String code = console.readCode("Código (ej: SALIDA): ", "código", 20);
        String name = console.readRequiredText("Nombre: ", "nombre", 100);
        String description = support.readOptional("Descripción", null,
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        BigDecimal amount = console.readMoney("Valor base en pesos (sin puntos, ej: 50000): ", "valor base");
        boolean recurring = console.confirm("¿Es un cobro mensual recurrente?");
        support.show(controller.createConcept(new PaymentConcept(null, code, name, description, amount, recurring, true)));
        console.pause();
    }

    private void updateConcept() {
        console.printTitle("Actualizar concepto");
        Optional<PaymentConcept> selected = pickConcept(false);
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        PaymentConcept concept = selected.get();
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        String name = support.readTextOrKeep("Nombre", concept.name(), true);
        String description = support.readOptional("Descripción", concept.description(),
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        BigDecimal amount = console.readValid("Valor base [" + concept.baseAmount().toPlainString() + "]: ",
                raw -> raw.isBlank() ? concept.baseAmount() : InputValidator.parseMoney(raw, "valor base"));
        boolean recurring = console.confirm("¿Es un cobro mensual recurrente?");
        boolean active = console.confirm("¿El concepto está activo?");
        support.show(controller.updateConcept(new PaymentConcept(concept.id(), concept.code(), name, description, amount,
                recurring, active)));
        console.pause();
    }

    private void addAdjustment() {
        console.printTitle("Valor personalizado, beca o descuento");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<PaymentConcept> concept = pickConcept(true);
        if (concept.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        AdjustmentType type = support.readEnum("Tipo de ajuste", AdjustmentType.class);
        BigDecimal percent = null;
        BigDecimal custom = null;
        if (type == AdjustmentType.CUSTOM_VALUE) {
            custom = console.readMoney("Valor personalizado en pesos: ", "valor personalizado");
        } else {
            percent = console.readValid("Porcentaje de descuento (1 a 100): ", raw -> {
                BigDecimal value = InputValidator.parseDecimal(raw, "porcentaje", 2);
                if (value.signum() <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new com.littlehorizons.erp.exception.ValidationException(
                            "El porcentaje debe ser mayor que 0 y máximo 100.");
                }
                return value;
            });
        }
        String notes = support.readOptional("Observaciones", null,
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        support.show(controller.addAdjustment(new PriceAdjustment(null, student.get().id(), concept.get().id(), null,
                year, type, percent, custom, notes)));
        support.quiet(controller.expectedAmount(student.get().id(), concept.get().id(), year))
                .ifPresent(amount -> console.printInfo("Valor a pagar con los ajustes: " + Formats.money(amount)));
        console.pause();
    }

    private void listAdjustments() {
        console.printTitle("Ajustes de un estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        Optional<List<PriceAdjustment>> adjustments = support.show(controller.findAdjustments(student.get().id(), year));
        if (adjustments.isEmpty() || adjustments.get().isEmpty()) {
            console.pause();
            return;
        }
        console.printTable(List.of("Id", "Concepto", "Tipo", "Descuento", "Valor personalizado", "Observaciones"),
                adjustments.get().stream().map(item -> List.of(String.valueOf(item.id()), item.conceptName(),
                        item.type().dbValue(), item.discountPercent() == null ? "-"
                                : item.discountPercent().stripTrailingZeros().toPlainString() + " %",
                        Formats.money(item.customAmount()), Formats.text(item.notes()))).collect(Collectors.toList()));
        if (console.confirm("¿Eliminar alguno de estos ajustes?")) {
            int id = console.readId("Id del ajuste: ", "id");
            if (adjustments.get().stream().noneMatch(item -> item.id() == id)) {
                console.printWarning("El id " + id + " no está en la lista.");
            } else if (console.confirm("¿Confirma eliminar el ajuste " + id + "? Los pagos registrados no cambian.")) {
                support.show(controller.removeAdjustment(id));
            }
        }
        console.pause();
    }

    // -------------------------------------------------------------- payments

    private void registerPayment() {
        console.printTitle("Registrar pago");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<PaymentConcept> concept = pickConcept(true);
        if (concept.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico al que aplica");
        support.quiet(controller.expectedAmount(student.get().id(), concept.get().id(), year))
                .ifPresent(amount -> console.printInfo("Valor esperado para este estudiante: " + Formats.money(amount)));
        BigDecimal amount = console.readMoney("Monto pagado en pesos (sin puntos): ", "monto pagado");
        LocalDate date = support.readDateOrToday("Fecha de pago");
        PaymentMethod method = support.readEnum("Método de pago", PaymentMethod.class);
        PaymentPeriod period = support.readEnum("Período al que aplica", PaymentPeriod.class);
        String receipt = support.readOptional("Número de recibo o comprobante (Enter para generarlo)", null,
                raw -> InputValidator.validateCode(raw, "recibo", 50));
        Integer guardianId = null;
        if (console.confirm("¿Pagó un acudiente distinto del responsable financiero principal?")) {
            guardianId = studentView.pickGuardian().map(Guardian::id).orElse(null);
        }
        String notes = support.readOptional("Notas", null, raw -> InputValidator.requireText(raw, "notas", 1000));
        PaymentService.PaymentRequest request = new PaymentService.PaymentRequest(student.get().id(), guardianId,
                concept.get().id(), amount, date, method, receipt, year, period, notes);
        if (!console.confirm("¿Confirmar el pago de " + Formats.money(amount) + " por " + concept.get().name() + "?")) {
            support.notFound();
            console.pause();
            return;
        }
        OperationResult<PaymentService.PaymentReceipt> result = controller.registerPayment(request, false);
        if (result.confirmationRequired()) {
            console.printWarning(result.message());
            if (!console.confirm("¿Es un pago diferente y desea registrarlo?")) {
                console.printInfo("No se registró ningún pago.");
                console.pause();
                return;
            }
            result = controller.registerPayment(request, true);
        }
        support.show(result).ifPresent(this::printReceipt);
        console.pause();
    }

    private void printReceipt(PaymentService.PaymentReceipt receipt) {
        BigDecimal balance = receipt.expectedAmount().subtract(receipt.payment().amount());
        if (balance.signum() > 0) {
            console.printInfo("Pago parcial: queda un saldo de " + Formats.money(balance) + " frente al valor esperado.");
        }
        InvoiceService.PdfResult pdf = receipt.pdf();
        if (pdf.generated()) {
            console.printSuccess("Factura PDF guardada en: " + pdf.path());
        } else {
            console.printError(pdf.errorMessage());
        }
        console.printInfo("CUFE simulado: " + receipt.invoice().cufe());
    }

    private void historyByStudent() {
        console.printTitle("Historial de pagos del estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Integer year = console.confirm("¿Filtrar por año académico?") ? support.readYear("Año") : null;
        support.show(controller.historyByStudent(student.get().id(), year)).ifPresent(this::printPayments);
        console.pause();
    }

    private void historyByGuardian() {
        console.printTitle("Historial de pagos del acudiente");
        Optional<Guardian> guardian = studentView.pickGuardian();
        if (guardian.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(controller.historyByGuardian(guardian.get().id())).ifPresent(this::printPayments);
        console.pause();
    }

    void printPayments(List<Payment> payments) {
        console.printTable(List.of("Recibo", "Fecha", "Estudiante", "Concepto", "Monto", "Método", "Año/Período", "Pagó"),
                payments.stream().map(payment -> List.of(Formats.text(payment.receiptNumber()),
                        Formats.date(payment.paymentDate()), payment.studentName(), payment.conceptName(),
                        Formats.money(payment.amount()), payment.method().dbValue(),
                        (payment.academicYear() == null ? "-" : payment.academicYear().toString())
                                + (payment.period() == null ? "" : " / " + payment.period().dbValue()),
                        Formats.text(payment.guardianName()))).collect(Collectors.toList()));
        BigDecimal total = payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        console.println("Total pagado: " + Formats.money(total));
    }

    private void statement() {
        console.printTitle("Estado de cuenta");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(controller.statement(student.get().id(), year)).ifPresent(statuses -> {
            console.printTable(List.of("Concepto", "Mes", "Valor esperado", "Pagado", "Saldo", "Estado"),
                    statuses.stream().map((ChargeStatus status) -> List.of(status.conceptName(),
                            status.month() == null ? "Año" : Month.of(status.month()).getDisplayName(TextStyle.FULL, SPANISH),
                            Formats.money(status.expected()), Formats.money(status.paid()),
                            Formats.money(status.balance()), status.state().label())).collect(Collectors.toList()));
            console.printInfo("Los conceptos mensuales se comparan por mes de pago; los demás por año.");
        });
        console.pause();
    }

    private void overdue() {
        console.printTitle("Obligaciones vencidas");
        int year = support.readYear("Año académico");
        support.show(controller.overdue(year)).ifPresent(this::printOverdue);
        console.printInfo("Regla: concepto mensual sin pago en más de un mes, o matrícula sin pagar completa "
                + "un mes después de matricularse.");
        console.pause();
    }

    void printOverdue(List<OverdueObligation> rows) {
        console.printTable(List.of("Documento", "Estudiante", "Grado", "Concepto", "Último pago / matrícula",
                        "Días de mora", "Pendiente", "Acudiente", "Teléfono"),
                rows.stream().map(row -> List.of(row.studentDocument(), row.studentName(), Formats.text(row.gradeName()),
                        row.conceptName(), Formats.date(row.referenceDate()), String.valueOf(row.daysOverdue()),
                        Formats.money(row.pendingAmount()), Formats.text(row.guardianName()),
                        Formats.text(row.guardianPhone()))).collect(Collectors.toList()));
    }

    private void regenerate() {
        console.printTitle("Regenerar facturas PDF pendientes");
        Optional<List<com.littlehorizons.erp.model.finance.Invoice>> pending = support.show(controller.pendingInvoices());
        if (pending.isEmpty() || pending.get().isEmpty()) {
            console.pause();
            return;
        }
        if (console.confirm("¿Generar ahora los PDF pendientes? No se crean pagos nuevos.")) {
            support.show(controller.regeneratePending()).ifPresent(results -> results.forEach(result -> {
                if (result.generated()) {
                    console.printSuccess(result.invoice().number() + ": " + result.path());
                } else {
                    console.printError(result.errorMessage());
                }
            }));
        }
        console.pause();
    }
}
