package com.littlehorizons.erp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Exercise 1: reads a student's grades with {@link Scanner} and writes them
 * to notas_&lt;documento&gt;.txt with {@link BufferedWriter}.
 *
 * <p>Every value is read with {@code nextLine()} and converted afterwards.
 * Mixing {@code nextInt()} with {@code nextLine()} leaves the Enter in the
 * buffer and the next {@code nextLine()} returns an empty text.</p>
 *
 * <p>Run it with Shift+F6 in NetBeans (Run File).</p>
 */
public final class EjercicioNotas {

    private static final BigDecimal PASSING_GRADE = new BigDecimal("3.0");
    private static final BigDecimal MAX_GRADE = new BigDecimal("5.0");

    private EjercicioNotas() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        String name = readText(scanner, "Nombre del estudiante: ");
        String document = readText(scanner, "Documento: ");
        int count = readCount(scanner);

        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            grades.add(readGrade(scanner, i));
        }
        BigDecimal average = grades.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
        boolean passed = average.compareTo(PASSING_GRADE) >= 0;

        Path file = Path.of("notas_" + document + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Estudiante: " + name);
            writer.newLine();
            writer.write("Documento: " + document);
            writer.newLine();
            for (int i = 0; i < grades.size(); i++) {
                writer.write("Nota " + (i + 1) + ": " + grades.get(i).toPlainString());
                writer.newLine();
            }
            writer.write("Promedio: " + average.toPlainString());
            writer.newLine();
            writer.write(passed ? "APROBADO" : "REPROBADO");
            writer.newLine();
        } catch (IOException e) {
            System.out.println("No se pudo escribir el archivo: " + e.getMessage());
            return;
        }
        System.out.println("Promedio " + average + " (" + (passed ? "APROBADO" : "REPROBADO") + ").");
        System.out.println("Archivo creado en " + file.toAbsolutePath());
    }

    private static String readText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().strip();
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("El valor es obligatorio.");
        }
    }

    private static int readCount(Scanner scanner) {
        while (true) {
            System.out.print("¿Cuántas notas va a ingresar? (1 a 5): ");
            String text = scanner.nextLine().strip();
            try {
                int count = Integer.parseInt(text);
                if (count >= 1 && count <= 5) {
                    return count;
                }
                System.out.println("Debe ser un número entre 1 y 5.");
            } catch (NumberFormatException e) {
                System.out.println("Escriba un número entero, por ejemplo 3.");
            }
        }
    }

    /** Accepts "4.5" and "4,5"; asks again until the grade is between 0.0 and 5.0. */
    private static BigDecimal readGrade(Scanner scanner, int number) {
        while (true) {
            System.out.print("Nota " + number + " (0.0 a 5.0): ");
            String text = scanner.nextLine().strip().replace(',', '.');
            try {
                BigDecimal grade = new BigDecimal(text);
                if (grade.signum() >= 0 && grade.compareTo(MAX_GRADE) <= 0 && grade.scale() <= 1) {
                    return grade.setScale(1, RoundingMode.UNNECESSARY);
                }
                System.out.println("La nota debe estar entre 0.0 y 5.0 con un decimal.");
            } catch (NumberFormatException e) {
                System.out.println("'" + text + "' no es un número. Escriba por ejemplo 4.5.");
            }
        }
    }
}
