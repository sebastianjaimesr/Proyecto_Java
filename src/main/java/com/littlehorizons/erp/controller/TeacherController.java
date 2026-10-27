package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.common.ImportResult;
import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.WeekDay;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates "Gestión de profesores" (teachers, assignments and schedules)
 * and the subject catalogue.
 */
public class TeacherController extends BaseController {

    private final TeacherService teacherService;
    private final SubjectService subjectService;

    public TeacherController(TeacherService teacherService, SubjectService subjectService) {
        this.teacherService = Objects.requireNonNull(teacherService, "teacherService");
        this.subjectService = Objects.requireNonNull(subjectService, "subjectService");
    }

    public OperationResult<Teacher> registerTeacher(Teacher draft) {
        return execute(() -> teacherService.register(draft),
                teacher -> "Profesor registrado con id " + teacher.id() + ".");
    }

    public OperationResult<Teacher> updateTeacher(Teacher changes) {
        return execute(() -> teacherService.update(changes), "Datos del profesor actualizados.");
    }

    public OperationResult<Teacher> findTeacher(int teacherId) {
        return execute(() -> teacherService.findById(teacherId), "Profesor encontrado.");
    }

    public OperationResult<List<Teacher>> searchTeachers(String text) {
        return execute(() -> teacherService.search(text), list -> list.size() + " profesor(es) encontrado(s).");
    }

    public OperationResult<TeacherAssignment> assign(int teacherId, int subjectId, int academicYear, String section) {
        return execute(() -> teacherService.assign(teacherId, subjectId, academicYear, section),
                assignment -> "Asignación " + assignment.id() + " registrada.");
    }

    public OperationResult<List<TeacherAssignment>> findAssignments(int teacherId, int academicYear) {
        return execute(() -> teacherService.findAssignments(teacherId, academicYear),
                list -> list.size() + " asignación(es).");
    }

    public OperationResult<List<TeacherAssignment>> findAssignmentsBySubject(int subjectId, int academicYear) {
        return execute(() -> teacherService.findAssignmentsBySubject(subjectId, academicYear),
                list -> list.size() + " asignación(es).");
    }

    public OperationResult<ClassSchedule> addSchedule(int assignmentId, WeekDay day, LocalTime start, LocalTime end,
                                                      String room) {
        return execute(() -> teacherService.addSchedule(assignmentId, day, start, end, room),
                "Horario registrado sin conflictos.");
    }

    public OperationResult<Boolean> removeSchedule(int scheduleId) {
        return execute(() -> {
            teacherService.removeSchedule(scheduleId);
            return Boolean.TRUE;
        }, "Horario eliminado.");
    }

    public OperationResult<List<ClassSchedule>> findTimetable(int teacherId, int academicYear) {
        return execute(() -> teacherService.findTimetable(teacherId, academicYear),
                list -> list.size() + " clase(s) semanales.");
    }

    // --------------------------------------------------------------- subjects

    public OperationResult<Subject> registerSubject(Subject draft) {
        return execute(() -> subjectService.register(draft),
                subject -> "Materia registrada con id " + subject.id() + ".");
    }

    public OperationResult<Subject> updateSubject(Subject changes) {
        return execute(() -> subjectService.update(changes), "Materia actualizada.");
    }

    public OperationResult<Subject> findSubject(int subjectId) {
        return execute(() -> subjectService.findById(subjectId), "Materia encontrada.");
    }

    public OperationResult<List<Subject>> findSubjectsByGrade(int gradeId, boolean onlyActive) {
        return execute(() -> subjectService.findByGrade(gradeId, onlyActive),
                list -> list.size() + " materia(s).");
    }

    public OperationResult<List<Subject>> findAllSubjects() {
        return execute(subjectService::findAll, list -> list.size() + " materia(s).");
    }

    public OperationResult<List<Grade>> findGrades() {
        return execute(subjectService::findGrades, "Grados cargados.");
    }

    // ------------------------------------------------------ text file exercises

    public OperationResult<Path> exportTimetable(int teacherId, int academicYear) {
        return execute(() -> teacherService.exportTimetable(teacherId, academicYear),
                path -> "Horario creado en " + path);
    }

    public OperationResult<ImportResult> importSchedules(Path file) {
        return execute(() -> teacherService.importSchedules(file), ImportResult::summary);
    }
}
