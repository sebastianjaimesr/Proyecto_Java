package test.view;

import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.Objects;

public final class SubjectExportView {

    private static final String HEADER = "EXPORTADOR DE MATERIAS ASIGNADAS - AÑO ";
    private static final String TEACHER_ID_PROMPT = "Ingrese el ID del profesor: ";
    private static final String INVALID_NUMBER = "Error: Debe ingresar un número válido.";
    private static final String TEACHER_NOT_FOUND = "Error: No se encontró ningún profesor con el ID %s.";
    private static final String TEACHER_WITHOUT_SUBJECTS = "Error: El profesor %s no tiene materias asignadas en %s.";
    private static final String EXPORT_SUCCEEDED = "Archivo generado exitosamente";
    private static final String TEACHER_LABEL = "Profesor: ";
    private static final String FILE_LABEL = "Archivo: ";
    private static final String EXPORT_FAILED = "Error: No se pudo crear el archivo de exportación.";
    private static final String DATABASE_ERROR = "Error: No se pudo consultar la base de datos. "
            + "Verifique que MySQL esté encendido y revise config.properties.";
    private static final String ERROR_PREFIX = "Error: ";
    private static final String UNEXPECTED_ERROR = "Error: Ocurrió un error inesperado. "
            + "Los detalles quedaron en el registro de errores.";
    private static final String INPUT_CLOSED = "Se cerró la entrada del teclado. El programa terminará.";

    private final ConsoleUtils console;

    public SubjectExportView(ConsoleUtils console) {
        this.console = Objects.requireNonNull(console, "console");
    }

    public void showHeader(int academicYear) {
        console.println(HEADER + academicYear);
    }

    public String readTeacherId() {
        return console.readLine(TEACHER_ID_PROMPT);
    }

    public void showInvalidNumber() {
        console.println(INVALID_NUMBER);
    }

    public void showTeacherNotFound(String teacherId) {
        console.println(String.format(TEACHER_NOT_FOUND, teacherId));
    }

    public void showTeacherWithoutSubjects(String fullName, int academicYear) {
        console.println(String.format(TEACHER_WITHOUT_SUBJECTS, fullName, String.valueOf(academicYear)));
    }

    public void showExportSucceeded(String fullName, String fileName) {
        console.println(EXPORT_SUCCEEDED);
        console.println(TEACHER_LABEL + fullName);
        console.println(FILE_LABEL + fileName);
    }

    public void showExportFailed() {
        console.println(EXPORT_FAILED);
    }

    public void showDatabaseError() {
        console.println(DATABASE_ERROR);
    }

    public void showConfigurationError(String message) {
        console.println(ERROR_PREFIX + message);
    }

    public void showUnexpectedError() {
        console.println(UNEXPECTED_ERROR);
    }

    public void showInputClosed() {
        console.println();
        console.println(INPUT_CLOSED);
    }
}
