package com.littlehorizons.erp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Exercise 2: asks for a path with {@link Scanner}, reads the file with
 * {@link BufferedReader} and shows each line numbered, the number of lines,
 * the blank lines and the longest line.
 *
 * <p>End of file: {@code BufferedReader.readLine()} returns {@code null};
 * with Scanner, {@code hasNextLine()} returns {@code false}.</p>
 */
public final class EjercicioLector {

    private EjercicioLector() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.print("Ruta del archivo (o 'salir'): ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String text = scanner.nextLine().strip();
            if (text.equalsIgnoreCase("salir")) {
                System.out.println("Hasta pronto.");
                return;
            }
            if (text.isEmpty()) {
                continue;
            }
            Path file = Path.of(text);
            if (!Files.isRegularFile(file)) {
                System.out.println("El archivo no existe: " + file.toAbsolutePath());
                continue;
            }
            printFile(file);
        }
    }

    private static void printFile(Path file) {
        int total = 0;
        int blank = 0;
        String longest = "";
        int longestNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                System.out.printf("%4d | %s%n", total, line);
                if (line.isBlank()) {
                    blank++;
                }
                if (line.length() > longest.length()) {
                    longest = line;
                    longestNumber = total;
                }
            }
        } catch (NoSuchFileException e) {
            System.out.println("El archivo no existe: " + file.toAbsolutePath());
            return;
        } catch (IOException e) {
            System.out.println("No se pudo leer el archivo: " + e.getMessage());
            return;
        }
        System.out.println("Total de líneas: " + total);
        System.out.println("Líneas vacías: " + blank);
        System.out.println(total == 0 ? "El archivo está vacío."
                : "Línea más larga: la " + longestNumber + " (" + longest.length() + " caracteres)");
    }
}
