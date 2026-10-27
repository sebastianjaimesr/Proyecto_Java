package test.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

public final class AtomicTextFileWriter {

    public Path write(Path target, List<String> lines) throws IOException {
        Path destination = target.toAbsolutePath().normalize();
        Path temporary = destination.resolveSibling(
                "." + destination.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            moveIntoPlace(temporary, destination);
            return destination;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(temporary, e);
            throw e;
        }
    }

    private static void moveIntoPlace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path file, Exception cause) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException cleanupError) {
            cause.addSuppressed(cleanupError);
        }
    }
}
