package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.Student;

import java.util.List;

/**
 * Detail screen of a student: personal data, guardians (emergency contacts are
 * guardians flagged as such), enrollment history and siblings (students that
 * share a guardian).
 */
public record StudentProfile(
        Student student,
        List<GuardianLink> guardians,
        List<Enrollment> enrollments,
        List<Student> siblings) {

    public StudentProfile {
        guardians = List.copyOf(guardians);
        enrollments = List.copyOf(enrollments);
        siblings = List.copyOf(siblings);
    }

    public List<GuardianLink> emergencyContacts() {
        return guardians.stream().filter(GuardianLink::emergencyContact).toList();
    }
}
