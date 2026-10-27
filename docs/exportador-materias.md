# Exportador de materias de un profesor (reto Java)

Genera la constancia `materias_profesor_<id>_2025.txt` con las materias que un profesor tiene asignadas en el año académico 2025. Todo el código está en el paquete `test` y no modifica ninguna clase existente del proyecto.

## Ejecución en NetBeans

1. Cargar `docs/database.sql` en MySQL 8 y después `docs/datos_exportador_materias_2025.sql` (los datos de prueba del proyecto son de 2026).
2. Copiar `config.example.properties` como `config.properties` y completar los datos de conexión.
3. Abrir `src/main/java/test/Main.java` y usar **Run File** (Shift + F6).

El archivo se crea en la carpeta raíz del proyecto y los errores técnicos quedan en `logs/error_log.txt`.

## Estructura

| Capa | Clases | Responsabilidad |
|---|---|---|
| `model` | `TeacherSummary`, `AssignedSubject`, `TeachingLoad` | Datos inmutables (records): nombre completo, documento y nombre del archivo |
| `dao` | `TeacherSummaryDao`, `AssignedSubjectDao` y sus implementaciones JDBC | Consultas parametrizadas sobre `profesores`, `asignaciones_profesor`, `materias` y `grados` |
| `controller` | `SubjectExportController`, `SubjectReportFormatter`, `SubjectReportExporter`, `AtomicTextFileWriter` | Validación del ID, armado del reporte y escritura del archivo |
| `view` | `SubjectExportView` | Mensajes y lectura del teclado |
| raíz | `Main` | Punto de entrada: configuración, registro de errores e inyección de dependencias |

## Clases reutilizadas del proyecto

`AppConfig`, `DatabaseConnection`, `JdbcTransactionManager`, `ConsoleUtils`, `AppLogger`, `PersonName`, `PersonDocumentType`, `DataAccessException`, `InputUnavailableException` y, en las pruebas, `TestDatabase`.

## Decisiones de diseño

- **Validación en el orden pedido:** primero se verifica que sea un número entero, luego que el profesor exista y por último que tenga materias en 2025.
- **Números fuera de rango:** un número entero con ceros a la izquierda o fuera del rango de `int` se trata como "no encontrado", no como "número inválido", porque sí es un número entero. Los IDs menores o iguales a cero no consultan la base.
- **Una conexión por intento:** el profesor y sus materias se leen en la misma conexión de solo lectura.
- **Sin duplicados:** una materia asignada en dos secciones aparece una sola vez, en el orden en que se asignó.
- **Escritura atómica:** el archivo se escribe primero en un temporal y luego se mueve, así nunca queda una constancia a medio escribir.
- **Columnas alineadas:** se usan anchos fijos de 10, 35 y 20 caracteres. Los textos más largos se recortan con "…" para no desalinear la tabla, y las tildes se normalizan antes de medir.
- **Fecha controlada:** la fecha se obtiene de un `Clock` inyectado, lo que permite probarla.

## Pruebas

Se ejecutan con `mvn test` o con **Test Project** (Alt + F6).

| Clase | Qué verifica |
|---|---|
| `TeacherSummaryTest` | Nombre completo sin dobles espacios, documento y nombre del archivo |
| `SubjectReportFormatterTest` | Contenido exacto del archivo, anchos de columna, recorte y tildes |
| `AtomicTextFileWriterTest` | UTF-8, reemplazo del archivo existente y limpieza ante fallos |
| `SubjectExportControllerTest` | Flujo completo: entradas inválidas, profesor inexistente, sin materias, error de escritura, base de datos caída y entrada cerrada |
| `JdbcSubjectExportIntegrationTest` | Consultas SQL reales: filtro por año, sin duplicados y orden; se omite si no existe `config.properties` |
