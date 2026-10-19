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

    public String fullName() {
        return name.fullName();
    }

    public Guardian withId(int newId) {
        return new Guardian(newId, documentType, documentNumber, name, relationship, mainPhone,
                secondaryPhone, email, address, occupation, financialResponsible);
    }
}
