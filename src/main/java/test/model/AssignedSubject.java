package test.model;

import java.util.Objects;

public record AssignedSubject(String code, String name, String grade) {

    public AssignedSubject {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(grade, "grade");
    }
}
