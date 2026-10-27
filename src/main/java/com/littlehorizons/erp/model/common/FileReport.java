package com.littlehorizons.erp.model.common;

import java.nio.file.Path;
import java.util.List;

/** Files written by an export option, with the folder that holds them. */
public record FileReport(Path folder, List<Path> files) {

    public FileReport {
        files = List.copyOf(files);
    }

    public String summary() {
        return files.size() + " archivo(s) creado(s) en " + folder.toAbsolutePath();
    }
}
