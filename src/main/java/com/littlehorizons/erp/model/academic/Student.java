package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.PersonName;

import java.time.LocalDate;
import java.time.Period;

/**
 * Student ({@code estudiantes}). Medical fields are sensitive and are shown
 * only on the detail screen, never in lists or logs.
 *
 * @param id {@code null} before the record is inserted
 */
public record Student(
        Integer id,
        StudentDocumentType documentType,
        String documentNumber,
        PersonName name,
        LocalDate birthDate,
        Gender gender,
        String address,
        String phone,
        String email,
        String bloodType,
        String allergies,
        String medicalConditions,
        LocalDate admissionDate,
        StudentStatus status) {

    public String fullName() {
        return name.fullName();
    }

    public int ageOn(LocalDate date) {
        return Period.between(birthDate, date).getYears();
    }

    public Student withId(int newId) {
        return new Student(newId, documentType, documentNumber, name, birthDate, gender, address, phone,
                email, bloodType, allergies, medicalConditions, admissionDate, status);
    }

    public Student withStatus(StudentStatus newStatus) {
        return new Student(id, documentType, documentNumber, name, birthDate, gender, address, phone,
                email, bloodType, allergies, medicalConditions, admissionDate, newStatus);
    }
}
