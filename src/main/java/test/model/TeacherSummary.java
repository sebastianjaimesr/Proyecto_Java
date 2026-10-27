package test.model;

import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.common.PersonName;

import java.util.Objects;
import java.util.regex.Pattern;

public record TeacherSummary(int id, PersonDocumentType documentType, String documentNumber, PersonName name) {

    private static final Pattern REPEATED_SPACES = Pattern.compile("\\s+");

    public TeacherSummary {
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(documentNumber, "documentNumber");
        Objects.requireNonNull(name, "name");
    }

    public String fullName() {
        return REPEATED_SPACES.matcher(name.fullName()).replaceAll(" ").strip();
    }

    public String document() {
        return documentType.dbValue() + " " + documentNumber.strip();
    }
}
