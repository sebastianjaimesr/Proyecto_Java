package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.academic.WeekDay;
import com.littlehorizons.erp.model.common.ImportResult;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Teachers, their subject assignments and weekly class schedule, including
 * basic timetable conflict detection.
 */
public class TeacherService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ROOM_LENGTH = 50;
    private static final int SEARCH_LIMIT = 100;
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("H:mm");

    private final TransactionManager transactions;
    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;
    private final TeacherAssignmentDao assignmentDao;
    private final Clock clock;

    public TeacherService(TransactionManager transactions, TeacherDao teacherDao, SubjectDao subjectDao,
                          TeacherAssignmentDao assignmentDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // --------------------------------------------------------------- teachers

    public Teacher register(Teacher draft) {
        Teacher teacher = normalize(draft);
        validate(teacher);
        Teacher active = new Teacher(null, teacher.documentType(), teacher.documentNumber(), teacher.name(),
                teacher.email(), teacher.phone(), teacher.specialization(), teacher.hireDate(), TeacherStatus.ACTIVE);
        return transactions.inTransaction(connection -> {
            ensureUnique(connection, active, null);
            return active.withId(teacherDao.insert(connection, active));
        });
    }

    /** Updates data and status (Activo, Inactivo, Licencia). Teachers are never deleted. */
    public Teacher update(Teacher changes) {
        Objects.requireNonNull(changes.id(), "id");
        if (changes.status() == null) {
            throw new ValidationException("El estado del profesor es obligatorio.");
        }
        Teacher teacher = normalize(changes);
        validate(teacher);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(teacherDao.findById(connection, teacher.id()),
                    "No existe un profesor con id " + teacher.id() + ".");
            ensureUnique(connection, teacher, teacher.id());
            teacherDao.update(connection, teacher);
            return teacher;
        });
    }

    public Teacher findById(int teacherId) {
        return transactions.read(connection -> requireTeacher(connection, teacherId));
    }

    public List<Teacher> search(String text) {
        return transactions.read(connection -> teacherDao.search(connection, text, SEARCH_LIMIT));
    }

    // ------------------------------------------------------------ assignments

    /** Assigns an active teacher to an active subject for a year and optional section. */
    public TeacherAssignment assign(int teacherId, int subjectId, int academicYear, String rawSection) {
        ServiceChecks.academicYear(academicYear, LocalDate.now(clock));
        String section = ServiceChecks.normalizeSection(rawSection);
        return transactions.inTransaction(connection -> {
            Teacher teacher = requireTeacher(connection, teacherId);
            if (teacher.status() != TeacherStatus.ACTIVE) {
                throw new BusinessRuleException("Solo se pueden asignar profesores en estado Activo.");
            }
            Subject subject = ServiceChecks.require(subjectDao.findById(connection, subjectId),
                    "No existe una materia con id " + subjectId + ".");
            if (!subject.active()) {
                throw new BusinessRuleException("La materia " + subject.name() + " está inactiva.");
            }
            if (assignmentDao.exists(connection, teacherId, subjectId, academicYear, section)) {
                throw new BusinessRuleException("El profesor ya tiene esa asignación para el año " + academicYear + ".");
            }
            int id = assignmentDao.insert(connection, new TeacherAssignment(null, teacherId, subjectId, academicYear,
                    section, teacher.fullName(), subject.name(), subject.gradeId()));
            return new TeacherAssignment(id, teacherId, subjectId, academicYear, section, teacher.fullName(),
                    subject.name(), subject.gradeId());
        });
    }

    public List<TeacherAssignment> findAssignments(int teacherId, int academicYear) {
        return transactions.read(connection -> {
            requireTeacher(connection, teacherId);
            return assignmentDao.findByTeacher(connection, teacherId, academicYear);
        });
    }

    public List<TeacherAssignment> findAssignmentsBySubject(int subjectId, int academicYear) {
        return transactions.read(connection -> assignmentDao.findBySubject(connection, subjectId, academicYear));
    }

    // -------------------------------------------------------------- schedules

    /**
     * Adds a weekly class slot. It is rejected when it overlaps another slot of
     * the same teacher, or of the same grade and section, in that year.
     */
    public ClassSchedule addSchedule(int assignmentId, WeekDay day, LocalTime start, LocalTime end, String room) {
        validateTimes(day, start, end);
        String normalizedRoom = InputValidator.optionalText(room, "aula", MAX_ROOM_LENGTH);
        return transactions.inTransaction(connection -> {
            TeacherAssignment assignment = ServiceChecks.require(assignmentDao.findById(connection, assignmentId),
                    "No existe una asignación con id " + assignmentId + ".");
            Optional<ClassSchedule> teacherClash = assignmentDao
                    .findTeacherSchedules(connection, assignment.teacherId(), assignment.academicYear(), day)
                    .stream().filter(slot -> slot.overlaps(day, start, end)).findFirst();
            if (teacherClash.isPresent()) {
                throw new BusinessRuleException("Conflicto de horario del profesor con: "
                        + describe(teacherClash.get()) + ".");
            }
            Optional<ClassSchedule> groupClash = assignmentDao
                    .findGroupSchedules(connection, assignment.gradeId(), assignment.section(),
                            assignment.academicYear(), day)
                    .stream().filter(slot -> slot.overlaps(day, start, end)).findFirst();
            if (groupClash.isPresent()) {
                throw new BusinessRuleException("Conflicto de horario del grupo con: "
                        + describe(groupClash.get()) + ".");
            }
            ClassSchedule draft = new ClassSchedule(null, assignmentId, day, start, end, normalizedRoom, null);
            int id = assignmentDao.insertSchedule(connection, draft);
            return new ClassSchedule(id, assignmentId, day, start, end, normalizedRoom,
                    assignment.subjectName() + " - " + assignment.teacherName());
        });
    }

    public void removeSchedule(int scheduleId) {
        transactions.inTransactionVoid(connection -> assignmentDao.deleteSchedule(connection, scheduleId));
    }

    public List<ClassSchedule> findTimetable(int teacherId, int academicYear) {
        return transactions.read(connection -> {
            requireTeacher(connection, teacherId);
            return assignmentDao.findTeacherTimetable(connection, teacherId, academicYear);
        });
    }

    public List<ClassSchedule> findSchedulesByAssignment(int assignmentId) {
        return transactions.read(connection -> assignmentDao.findSchedulesByAssignment(connection, assignmentId));
    }

    // ------------------------------------------------------ text file exercises

    /**
     * Exercise 7: weekly timetable grouped by day, Monday to Saturday (enum
     * order, not alphabetical), each day sorted by start time, with the total
     * weekly hours.
     */
    public Path exportTimetable(int teacherId, int academicYear) {
        Teacher teacher = transactions.read(connection -> requireTeacher(connection, teacherId));
        List<ClassSchedule> schedules = findTimetable(teacherId, academicYear);
        if (schedules.isEmpty()) {
            throw new BusinessRuleException("El profesor no tiene horarios registrados en " + academicYear + ".");
        }
        Map<WeekDay, List<ClassSchedule>> byDay = schedules.stream()
                .collect(Collectors.groupingBy(ClassSchedule::day, () -> new EnumMap<>(WeekDay.class),
                        Collectors.toList()));
        List<String> lines = new ArrayList<>();
        lines.add("HORARIO SEMANAL " + academicYear);
        lines.add("Profesor: " + teacher.fullName() + " (" + Formats.text(teacher.specialization()) + ")");
        lines.add("");
        byDay.forEach((day, slots) -> {
            lines.add(day.dbValue().toUpperCase(Locale.ROOT));
            slots.stream().sorted(Comparator.comparing(ClassSchedule::startTime))
                    .forEach(slot -> lines.add(String.format("  %5s - %5s  %-12s %s", HOUR.format(slot.startTime()),
                            HOUR.format(slot.endTime()), Formats.text(slot.room()), slot.description())));
            lines.add("");
        });
        long minutes = schedules.stream()
                .mapToLong(slot -> Duration.between(slot.startTime(), slot.endTime()).toMinutes()).sum();
        lines.add("Total semanal: " + (minutes / 60) + " h " + (minutes % 60) + " min en " + schedules.size()
                + " bloque(s).");
        Path file = FileSupport.EXPORTS.resolve("horarios")
                .resolve("horario_" + teacher.documentNumber() + "_" + academicYear + ".txt");
        return FileSupport.write(file, lines, TeacherService.class);
    }

    /**
     * Exercise 18: loads idAsignacion;dia;horaInicio;horaFin;aula lines with
     * {@link Scanner} and {@code useDelimiter} (no split). Each slot goes
     * through {@link #addSchedule}, which detects teacher and group clashes.
     */
    public ImportResult importSchedules(Path file) {
        FileSupport.requireFile(file);
        List<String> errors = new ArrayList<>();
        int processed = 0;
        int saved = 0;
        try (Scanner fileScanner = new Scanner(file, StandardCharsets.UTF_8)) {
            int number = 0;
            while (fileScanner.hasNextLine()) {
                number++;
                String text = fileScanner.nextLine().strip();
                if (number == 1 && text.startsWith("\uFEFF")) {
                    text = text.substring(1);
                }
                if (text.isEmpty() || text.startsWith("#")) {
                    continue;
                }
                processed++;
                try (Scanner fields = new Scanner(text).useDelimiter("\\s*;\\s*")) {
                    int assignmentId = fields.nextInt();
                    WeekDay day = FileSupport.parseEnum(fields.next(), WeekDay.class, "día");
                    LocalTime start = LocalTime.parse(fields.next(), HOUR);
                    LocalTime end = LocalTime.parse(fields.next(), HOUR);
                    String room = fields.hasNext() ? fields.next() : null;
                    addSchedule(assignmentId, day, start, end, room);
                    saved++;
                } catch (InputMismatchException e) {
                    // Subclass of NoSuchElementException, so it must be caught first.
                    errors.add(FileSupport.lineError(number, "el id de la asignación debe ser un número entero."));
                } catch (NoSuchElementException e) {
                    errors.add(FileSupport.lineError(number,
                            "faltan campos (idAsignacion;dia;horaInicio;horaFin;aula)."));
                } catch (DateTimeParseException e) {
                    errors.add(FileSupport.lineError(number, "hora no válida '" + e.getParsedString()
                            + "' (use HH:MM, por ejemplo 7:00 o 14:30)."));
                } catch (ValidationException | BusinessRuleException e) {
                    errors.add(FileSupport.lineError(number, e.getMessage()));
                }
            }
        } catch (IOException e) {
            AppLogger.logError(TeacherService.class, "Could not read " + file, e);
            throw new BusinessRuleException("No fue posible leer el archivo " + file.getFileName() + ".");
        }
        return new ImportResult(processed, saved, errors);
    }

    static void validateTimes(WeekDay day, LocalTime start, LocalTime end) {
        if (day == null || start == null || end == null) {
            throw new ValidationException("El día, la hora de inicio y la hora de fin son obligatorios.");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (start.isBefore(ServiceChecks.SCHOOL_DAY_START) || end.isAfter(ServiceChecks.SCHOOL_DAY_END)) {
            throw new ValidationException("Las clases deben estar entre las "
                    + ServiceChecks.SCHOOL_DAY_START + " y las " + ServiceChecks.SCHOOL_DAY_END + ".");
        }
    }

    private static String describe(ClassSchedule slot) {
        return slot.description() + " (" + slot.day().dbValue() + " " + slot.startTime() + "-" + slot.endTime() + ")";
    }

    private Teacher requireTeacher(Connection connection, int teacherId) throws SQLException {
        return ServiceChecks.require(teacherDao.findById(connection, teacherId),
                "No existe un profesor con id " + teacherId + ".");
    }

    private void ensureUnique(Connection connection, Teacher teacher, Integer ownId) throws SQLException {
        Optional<Teacher> sameDocument = teacherDao.findByDocument(connection, teacher.documentNumber())
                .filter(existing -> !existing.id().equals(ownId));
        if (sameDocument.isPresent()) {
            throw new BusinessRuleException("Ya existe un profesor con el documento " + teacher.documentNumber() + ".");
        }
        Optional<Teacher> sameEmail = teacherDao.findByEmail(connection, teacher.email())
                .filter(existing -> !existing.id().equals(ownId));
        if (sameEmail.isPresent()) {
            throw new BusinessRuleException("El correo " + teacher.email() + " ya pertenece a otro profesor.");
        }
    }

    private static Teacher normalize(Teacher teacher) {
        return new Teacher(teacher.id(), teacher.documentType(),
                InputValidator.validateDocumentNumber(teacher.documentNumber()), teacher.name(),
                InputValidator.validateEmail(teacher.email(), "correo electrónico"),
                teacher.phone() == null || teacher.phone().isBlank() ? null : teacher.phone().strip(),
                teacher.specialization() == null || teacher.specialization().isBlank()
                        ? null : teacher.specialization().strip(),
                teacher.hireDate(), teacher.status());
    }

    private void validate(Teacher teacher) {
        if (teacher.documentType() == null) {
            throw new ValidationException("El tipo de documento es obligatorio.");
        }
        ServiceChecks.requireText(teacher.name().firstName(), "primer nombre", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(teacher.name().middleName(), "segundo nombre", MAX_NAME_LENGTH);
        ServiceChecks.requireText(teacher.name().lastName(), "primer apellido", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(teacher.name().secondLastName(), "segundo apellido", MAX_NAME_LENGTH);
        ServiceChecks.optionalPhone(teacher.phone(), "teléfono");
        ServiceChecks.optionalText(teacher.specialization(), "especialización", MAX_NAME_LENGTH);
        ServiceChecks.notInFuture(teacher.hireDate(), LocalDate.now(clock), "fecha de contratación");
    }
}
