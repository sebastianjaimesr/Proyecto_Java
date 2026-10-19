package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.PersonName;

/**
 * Acudiente o tutor legal ({@code acudientes}).
 *
 * @param id {@code null} antes de insertar el registro
 */
public record Guardian(
        Integer id,
        PersonDocumentType documentType,
        String documentNumber,
        PersonName name,
        String relationship,
        String mainPhone,
        String secondaryPhone,
        String email,
        String address,
        String occupation,
        boolean financialResponsible) {

    /** Nombre completo del acudiente. */
    public String fullName() {
        return name.fullName();
    }

    /** Copia del acudiente con el id asignado por la base de datos (los records son inmutables). */
    public Guardian withId(int newId) {
        return new Guardian(newId, documentType, documentNumber, name, relationship, mainPhone,
                secondaryPhone, email, address, occupation, financialResponsible);
    }
}
