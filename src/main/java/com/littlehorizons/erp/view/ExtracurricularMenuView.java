package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;
import com.littlehorizons.erp.model.extracurricular.CompetitionPosition;
import com.littlehorizons.erp.model.extracurricular.Event;
import com.littlehorizons.erp.model.extracurricular.EventParticipant;
import com.littlehorizons.erp.model.extracurricular.EventType;
import com.littlehorizons.erp.model.extracurricular.ParticipationRole;
import com.littlehorizons.erp.service.ClubService;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pantallas de "4. Actividades extracurriculares".
 */
public class ExtracurricularMenuView {

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final ExtracurricularController controller;
    private final StudentMenuView studentView;
    private final TeacherMenuView teacherView;

    public ExtracurricularMenuView(ViewSupport support, ExtracurricularController controller,
                                   StudentMenuView studentView, TeacherMenuView teacherView) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.controller = Objects.requireNonNull(controller, "controller");
        this.studentView = Objects.requireNonNull(studentView, "studentView");
        this.teacherView = Objects.requireNonNull(teacherView, "teacherView");
    }

    /** Muestra el submenú de "Actividades extracurriculares". */
    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Clubes", this::showClubsMenu),
                        new Menu.Item("Eventos y competencias", this::showEventsMenu)),
                "ACTIVIDADES EXTRACURRICULARES").run();
    }

    /** Submenú de clubes. */
    private void showClubsMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Crear club", this::createClub),
                        new Menu.Item("Consultar catálogo de clubes", this::listClubs),
                        new Menu.Item("Actualizar club", this::updateClub),
                        new Menu.Item("Inscribir estudiante en un club", this::enroll),
                        new Menu.Item("Retirar estudiante de un club", this::withdraw),
                        new Menu.Item("Consultar miembros de un club", this::listMembers),
                        new Menu.Item("Consultar clubes de un estudiante", this::clubsOfStudent)),
                "CLUBES").run();
    }

    /** Submenú de eventos y competencias. */
    private void showEventsMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Registrar evento", this::createEvent),
                        new Menu.Item("Consultar eventos de un año", this::listEvents),
                        new Menu.Item("Actualizar evento", this::updateEvent),
                        new Menu.Item("Registrar participante", this::addParticipant),
                        new Menu.Item("Registrar posición en una competencia", this::recordResult),
                        new Menu.Item("Consultar participantes y resultados de un evento", this::listParticipants),
                        new Menu.Item("Consultar participación histórica de un estudiante", this::eventsOfStudent)),
                "EVENTOS Y COMPETENCIAS").run();
    }

    // ----------------------------------------------------------------- clubes

    /** Muestra los clubes y deja elegir uno por su id. */
    private Optional<Club> pickClub(boolean onlyActive) {
        Optional<List<Club>> clubs = support.quiet(controller.findClubs(onlyActive));
        if (clubs.isEmpty() || clubs.get().isEmpty()) {
            console.printWarning("No hay clubes registrados.");
            return Optional.empty();
        }
        console.println("Club:");
        return Optional.of(console.readChoice("Opción: ", clubs.get(),
                club -> club.name() + " (" + club.activeMembers() + "/" + club.capacity() + ")"
                        + (club.active() ? "" : " - inactivo"), "club"));
    }

    /** Pregunta si se asigna un profesor encargado y, si es así, deja elegirlo. */
    private Integer pickSupervisor(Integer current) {
        if (!console.confirm("¿Asignar profesor encargado?")) {
            return current;
        }
        return teacherView.pickTeacher().map(Teacher::id).orElse(current);
    }

    /** Pantalla para crear un club. */
    private void createClub() {
        console.printTitle("Crear club");
        String name = console.readRequiredText("Nombre del club: ", "nombre", 100);
        String description = support.readOptional("Descripción", null,
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        int capacity = console.readValid("Cupo máximo [" + Club.DEFAULT_CAPACITY + "]: ", raw -> raw.isBlank()
                ? Club.DEFAULT_CAPACITY : InputValidator.parseInt(raw, "cupo máximo", 1, ClubService.MAX_CAPACITY));
        String schedule = support.readOptional("Horario de reunión (ej: Martes 3-5 PM)", null,
                raw -> InputValidator.requireText(raw, "horario", 100));
        Integer supervisor = pickSupervisor(null);
        support.show(controller.createClub(new Club(null, name, description, supervisor, null, capacity, schedule,
                true, 0)));
        console.pause();
    }

    /** Pantalla para actualizar un club. */
    private void updateClub() {
        console.printTitle("Actualizar club");
        Optional<Club> selected = pickClub(false);
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        Club club = selected.get();
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        String name = support.readTextOrKeep("Nombre", club.name(), true);
        String description = support.readOptional("Descripción", club.description(),
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        int capacity = console.readValid("Cupo máximo [" + club.capacity() + "]: ", raw -> raw.isBlank()
                ? club.capacity() : InputValidator.parseInt(raw, "cupo máximo", 1, ClubService.MAX_CAPACITY));
        String schedule = support.readOptional("Horario de reunión", club.meetingSchedule(),
                raw -> InputValidator.requireText(raw, "horario", 100));
        Integer supervisor = pickSupervisor(club.supervisorId());
        boolean active = console.confirm("¿El club está activo?");
        support.show(controller.updateClub(new Club(club.id(), name, description, supervisor, null, capacity,
                schedule, active, club.activeMembers())));
        console.pause();
    }

    /** Muestra el catálogo de clubes con su ocupación. */
    private void listClubs() {
        console.printTitle("Catálogo de clubes");
        support.show(controller.findClubs(false)).ifPresent(clubs -> console.printTable(
                List.of("Id", "Club", "Encargado", "Horario", "Miembros", "Cupos libres", "Activo"),
                clubs.stream().map(club -> List.of(String.valueOf(club.id()), club.name(),
                        Formats.text(club.supervisorName()), Formats.text(club.meetingSchedule()),
                        club.activeMembers() + "/" + club.capacity(), String.valueOf(club.availableSeats()),
                        Formats.yesNo(club.active()))).collect(Collectors.toList())));
        console.pause();
    }

    /** Inscribe a un estudiante en un club. */
    private void enroll() {
        console.printTitle("Inscribir en club");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Club> club = pickClub(true);
        if (club.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate date = support.readDateOrToday("Fecha de ingreso");
        support.show(controller.enrollInClub(student.get().id(), club.get().id(), date));
        console.pause();
    }

    /** Retira a un estudiante de un club, con fecha y motivo. */
    private void withdraw() {
        console.printTitle("Retirar de un club");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        List<ClubMembership> active = support.quiet(controller.findClubsOfStudent(student.get().id()))
                .orElse(List.of()).stream()
                .filter(membership -> membership.status() == com.littlehorizons.erp.model.extracurricular
                        .MembershipStatus.ACTIVE)
                .collect(Collectors.toList());
        if (active.isEmpty()) {
            console.printWarning("El estudiante no tiene membresías activas.");
            console.pause();
            return;
        }
        ClubMembership membership = console.readChoice("Membresía: ", active, ClubMembership::clubName, "membresía");
        LocalDate date = support.readDateOrToday("Fecha de retiro");
        String reason = support.readOptional("Motivo", null, raw -> InputValidator.requireText(raw, "motivo", 1000));
        support.show(controller.withdrawFromClub(membership.id(), date, reason));
        console.pause();
    }

    /** Muestra los miembros de un club. */
    private void listMembers() {
        console.printTitle("Miembros del club");
        Optional<Club> club = pickClub(false);
        if (club.isEmpty()) {
            support.notFound();
            return;
        }
        boolean onlyActive = console.confirm("¿Mostrar solo miembros activos?");
        support.show(controller.findMembers(club.get().id(), onlyActive)).ifPresent(this::printMemberships);
        console.pause();
    }

    /** Muestra los clubes de un estudiante (historial). */
    private void clubsOfStudent() {
        console.printTitle("Clubes del estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(controller.findClubsOfStudent(student.get().id())).ifPresent(this::printMemberships);
        console.pause();
    }

    /** Imprime una tabla de membresías. */
    private void printMemberships(List<ClubMembership> memberships) {
        console.printTable(List.of("Id", "Estudiante", "Club", "Ingreso", "Estado", "Retiro", "Motivo"),
                memberships.stream().map(membership -> List.of(String.valueOf(membership.id()),
                        membership.studentName(), membership.clubName(), Formats.date(membership.joinDate()),
                        membership.status().dbValue(), Formats.date(membership.withdrawalDate()),
                        Formats.text(membership.withdrawalReason()))).collect(Collectors.toList()));
    }

    // ---------------------------------------------------------------- eventos

    /** Pide el año, muestra sus eventos y deja elegir uno. */
    private Optional<Event> pickEvent() {
        int year = support.readYear("Año del evento");
        Optional<List<Event>> events = support.quiet(controller.findEvents(year, null));
        if (events.isEmpty() || events.get().isEmpty()) {
            console.printWarning("No hay eventos registrados en " + year + ".");
            return Optional.empty();
        }
        console.println("Evento:");
        return Optional.of(console.readChoice("Opción: ", events.get(),
                event -> Formats.date(event.date()) + " - " + event.name() + " (" + event.type().dbValue() + ")",
                "evento"));
    }

    /** Pide los datos de un evento; si {@code current} existe, Enter conserva cada valor actual. */
    private Event readEvent(Event current) {
        String name = support.readTextOrKeep("Nombre del evento", current == null ? null : current.name(), true);
        String description = support.readOptional("Descripción", current == null ? null : current.description(),
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        EventType type = support.readEnum("Tipo de evento", EventType.class);
        LocalDate date = console.readValid("Fecha (AAAA-MM-DD)" + (current == null ? "" : " [" + current.date() + "]")
                + ": ", raw -> raw.isBlank() && current != null ? current.date()
                : InputValidator.parseDate(raw, "fecha del evento"));
        String place = support.readOptional("Lugar", current == null ? null : current.place(),
                raw -> InputValidator.requireText(raw, "lugar", 150));
        boolean competition = console.confirm("¿Tiene competencia con posiciones?");
        Integer organizer = current == null ? null : current.organizerId();
        if (console.confirm("¿Asignar profesor organizador?")) {
            organizer = teacherView.pickTeacher().map(Teacher::id).orElse(organizer);
        }
        boolean active = current == null || console.confirm("¿El evento está activo?");
        return new Event(current == null ? null : current.id(), name, description, type, date, place, competition,
                organizer, null, active);
    }

    /** Pantalla para registrar un evento. */
    private void createEvent() {
        console.printTitle("Registrar evento");
        support.show(controller.createEvent(readEvent(null)));
        console.pause();
    }

    /** Pantalla para actualizar un evento. */
    private void updateEvent() {
        console.printTitle("Actualizar evento");
        Optional<Event> event = pickEvent();
        if (event.isEmpty()) {
            support.notFound();
            return;
        }
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        support.show(controller.updateEvent(readEvent(event.get())));
        console.pause();
    }

    /** Muestra los eventos de un año, opcionalmente de un tipo. */
    private void listEvents() {
        console.printTitle("Eventos");
        int year = support.readYear("Año");
        EventType type = console.confirm("¿Filtrar por tipo?") ? support.readEnum("Tipo", EventType.class) : null;
        support.show(controller.findEvents(year, type)).ifPresent(events -> console.printTable(
                List.of("Id", "Fecha", "Evento", "Tipo", "Lugar", "Competencia", "Organizador", "Activo"),
                events.stream().map(event -> List.of(String.valueOf(event.id()), Formats.date(event.date()),
                        event.name(), event.type().dbValue(), Formats.text(event.place()),
                        Formats.yesNo(event.competition()), Formats.text(event.organizerName()),
                        Formats.yesNo(event.active()))).collect(Collectors.toList())));
        console.pause();
    }

    /** Inscribe a un estudiante como participante de un evento. */
    private void addParticipant() {
        console.printTitle("Registrar participante");
        Optional<Event> event = pickEvent();
        if (event.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        ParticipationRole role = support.readEnum("Rol", ParticipationRole.class);
        String notes = support.readOptional("Observaciones", null,
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        support.show(controller.addParticipant(event.get().id(), student.get().id(), role, notes));
        console.pause();
    }

    /** Registra la posición de un participante en una competencia. */
    private void recordResult() {
        console.printTitle("Registrar posición");
        Optional<Event> event = pickEvent();
        if (event.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<List<EventParticipant>> participants = support.quiet(controller.findParticipants(event.get().id()));
        if (participants.isEmpty() || participants.get().isEmpty()) {
            console.printWarning("El evento no tiene participantes.");
            console.pause();
            return;
        }
        EventParticipant participant = console.readChoice("Participante: ", participants.get(),
                item -> item.studentName() + (item.position() == null ? "" : " (" + item.position().dbValue() + ")"),
                "participante");
        CompetitionPosition position = support.readEnum("Posición", CompetitionPosition.class);
        String notes = support.readOptional("Observaciones", participant.notes(),
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        support.show(controller.recordResult(event.get().id(), participant.studentId(), position, notes));
        console.pause();
    }

    /** Muestra los participantes y resultados de un evento. */
    private void listParticipants() {
        console.printTitle("Participantes y resultados");
        Optional<Event> event = pickEvent();
        if (event.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(controller.findParticipants(event.get().id())).ifPresent(this::printParticipants);
        console.pause();
    }

    /** Muestra los eventos en los que ha participado un estudiante. */
    private void eventsOfStudent() {
        console.printTitle("Participación histórica");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(controller.findEventsOfStudent(student.get().id())).ifPresent(this::printParticipants);
        console.pause();
    }

    /** Imprime una tabla de participaciones (también la usa la vista de reportes). */
    void printParticipants(List<EventParticipant> participants) {
        console.printTable(List.of("Fecha", "Evento", "Tipo", "Estudiante", "Rol", "Posición"),
                participants.stream().map(item -> List.of(Formats.date(item.eventDate()), item.eventName(),
                        item.eventType().dbValue(), item.studentName(), item.role().dbValue(),
                        item.position() == null ? "-" : item.position().dbValue())).collect(Collectors.toList()));
    }
}
