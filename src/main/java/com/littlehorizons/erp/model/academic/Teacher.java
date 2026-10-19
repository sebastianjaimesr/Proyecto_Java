package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.PersonName;

import java.time.LocalDate;

/**
 * Profesor ({@code profesores}).
 *
 * @param id {@code null} antes de insertar el registro
 */
public record Teacher(
        Integer id,
        PersonDocumentType documentType,
        String documentNumber,
        PersonName name,
        String email,
        String phone,
        String specialization,
        LocalDate hireDate,
        TeacherStatus status) {

    public String fullName() {
        return name.fullName();
    }

    public Teacher withId(int newId) {
        return new Teacher(newId, documentType, documentNumber, name, email, phone, specialization, hireDate, status);
    }
}
