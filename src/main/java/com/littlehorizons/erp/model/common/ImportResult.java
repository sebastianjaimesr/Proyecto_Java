package com.littlehorizons.erp.model.common;

import java.util.List;

/**
 * Outcome of reading a data file: data lines read, records saved and one
 * message per rejected line ("Línea 4: ...").
 */
public record ImportResult(int processed, int saved, List<String> errors) {

    public ImportResult {
        errors = List.copyOf(errors);
    }

    public String summary() {
        return processed + " línea(s) procesada(s), " + saved + " guardada(s), " + errors.size()
                + " con error.";
    }
}
