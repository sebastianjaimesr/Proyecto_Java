package com.littlehorizons.erp.util;

import java.text.Normalizer;
import java.util.Locale;

/** Builds file names that are valid on Windows, macOS and Linux. */
public final class FileNames {

    private FileNames() {
    }

    /**
     * "Robótica Junior #2" becomes "robotica_junior_2": no accents, spaces or
     * symbols.
     */
    public static String safe(String text) {
        if (text == null || text.isBlank()) {
            return "sin_nombre";
        }
        String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
        return slug.isEmpty() ? "sin_nombre" : slug;
    }
}
