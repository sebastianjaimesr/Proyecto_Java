# Guía de estudio: ERP Colegio Little Horizons (Proyecto_Java)

Guía para entender, explicar y sustentar este proyecto. Complementa el `README.md` (que explica instalación, reglas de negocio y limitaciones) y los comentarios en español que tiene cada clase y método del código.

---

## 1. ¿Qué es este proyecto?

Un **ERP de consola** (sistema de gestión) para un colegio, de preescolar a grado 11.º. Reúne en un solo programa:

- Estudiantes, acudientes y matrículas.
- Materias, inscripciones, notas, promedios, historial y asistencia.
- Profesores, asignaciones y horarios (con detección de cruces).
- Clubes (con cupo) y eventos/competencias.
- Finanzas: conceptos de pago, becas y descuentos, pagos, facturas PDF con CUFE y QR simulados, morosidad.
- Inventario de recursos y préstamos.
- 14 reportes y un menú de estado del sistema.

Se usa desde la terminal con menús numerados y guarda todo en **MySQL**.

## 2. Lenguajes y tecnologías

| Tecnología | Para qué se usa aquí |
|---|---|
| **Java 17** | Todo el programa. Usa `record`, `enum`, lambdas y Stream API. |
| **MySQL 8** | Base de datos `erp_little_horizons` (script en `docs/database.sql`). |
| **SQL** | Consultas dentro de los DAO, siempre con `PreparedStatement`. |
| **JDBC** (MySQL Connector/J) | Conexión entre Java y MySQL. |
| **Maven** (`pom.xml`) | Compila el proyecto y descarga las librerías. |
| **OpenPDF** | Genera la factura en PDF. |
| **ZXing** | Genera el código QR de la factura. |
| **JUnit 5** | Pruebas automáticas (unitarias y de integración). |
| **java.util.logging** | Guarda los errores técnicos en `logs/error_log.txt`. |

## 3. Arquitectura: MVC en capas

El flujo es **siempre** el mismo:

```
Vista (view) → Controlador (controller) → Servicio (service) → DAO (dao) → MySQL
```

| Capa | Paquete | Qué hace | Qué NO hace |
|---|---|---|---|
| **Vista** | `view` | Muestra menús, pide datos, imprime tablas. | No tiene SQL ni reglas de negocio. |
| **Controlador** | `controller` | Llama al servicio y convierte el resultado (o el error) en un `OperationResult` con mensaje en español. | No escribe en la consola. |
| **Servicio** | `service` | Reglas del colegio, validaciones y transacciones. | No conoce la consola. |
| **DAO** | `dao` + `dao/jdbc` | El SQL: insertar, buscar, actualizar y convertir filas en objetos. | No decide reglas. |
| **Modelo** | `model` | Los datos (`record`) y los valores fijos (`enum`). | No tiene lógica pesada. |
| **Utilidades** | `util` | Configuración, conexión, consola, validación, logs, PDF, QR, CUFE. | |
| **Excepciones** | `exception` | Errores propios del sistema. | |

**Frase para sustentar:** "Ninguna vista usa DAOs, ningún controlador escribe en consola y solo los DAO tienen SQL. Así cada capa se puede cambiar o probar sin tocar las demás."

## 4. ¿Qué pasa cuando se ejecuta? (arranque)

1. `Main.main()` lee `config.properties` con `AppConfig` (Singleton).
2. Prepara la consola (`ConsoleUtils`) y el registro de errores (`AppLogger`).
3. `run()` crea la `ApplicationFactory`, que construye **todo** el sistema: DAOs → servicios → controladores → vistas, pasando cada dependencia por el constructor.
4. `SystemMenuView.printStartupStatus()` prueba la conexión a MySQL y verifica que existan las tablas oficiales.
5. `MainMenuView.show()` muestra el menú principal (opciones 1 a 9) hasta que el usuario elige "Salir".
6. Al salir, el bloque `finally` cierra la consola y el archivo de log.

## 5. Ejemplo completo: registrar un pago

Sigue este recorrido abriendo cada archivo; es el mejor ejemplo para la sustentación.

1. **Vista** — `FinanceMenuView.registerPayment()`: elige al estudiante, el concepto, pide monto, fecha, método y recibo, y llama a `controller.registerPayment(request, false)`.
2. **Controlador** — `FinanceController.registerPayment()`: llama a `paymentService.register(...)` dentro de `execute(...)`.
3. **Servicio** — `PaymentService.register()`:
   - Valida los datos (`validate`).
   - Abre una **transacción** con `transactions.inTransaction(connection -> { ... })`.
   - Verifica que existan el estudiante y el concepto, y que el concepto esté activo.
   - Decide qué acudiente pagó (`resolvePayer`).
   - Rechaza un recibo repetido y, si hay un pago idéntico, lanza `ConfirmationRequiredException`.
   - Inserta el pago y crea la factura (número + CUFE) **en la misma transacción**.
   - Fuera de la transacción, genera el PDF.
4. **DAO** — `JdbcPaymentDao.insert()` y `JdbcInvoiceDao.insert()` ejecutan el `INSERT` con parámetros `?`.
5. **De vuelta en el controlador** — si todo salió bien, devuelve `OperationResult.ok("Pago registrado con recibo ... y factura ...")`. Si fue un duplicado, devuelve `needsConfirmation`, y la vista le pregunta al usuario si quiere registrarlo igual (se reintenta con `confirmDuplicate = true`).

## 6. Conceptos clave que debes dominar

**Transacción.** Varias operaciones que se guardan **todas o ninguna**. `JdbcTransactionManager.inTransaction()` desactiva el auto-commit, ejecuta el trabajo, hace `commit()` si todo sale bien y `rollback()` si hay error. La conexión se cierra siempre con *try-with-resources*.

**`PreparedStatement` y SQL injection.** Los valores del usuario nunca se pegan dentro del texto SQL: se pasan como parámetros `?`. Así un texto malicioso no puede cambiar la consulta.

**`SELECT ... FOR UPDATE`.** Bloquea una fila mientras dura la transacción. Se usa en el cupo de clubes (`ClubDao.lockById`) y en los préstamos: si dos personas inscriben al mismo tiempo, la segunda espera y ve el conteo actualizado, así nunca se pasa del cupo.

**`OperationResult`.** Un `record` con `success`, `message`, `data` y `confirmationRequired`. Es la forma en que el controlador le responde a la vista sin lanzar excepciones.

**Manejo de errores (`BaseController.execute`).**
- `ValidationException` / `BusinessRuleException` → el mensaje se muestra tal cual al usuario.
- `ConfirmationRequiredException` → la vista pide confirmación.
- `DataAccessException` (error técnico) → se guarda en `error_log.txt` y el usuario ve un mensaje genérico, sin SQL ni *stack trace*.

**Errores de MySQL traducidos.** `JdbcTransactionManager.translate()` convierte códigos como 1062 (duplicado) o 1452 (clave foránea inexistente) en mensajes de negocio en español.

**`record`.** Clase inmutable de solo datos (ej. `Student`, `Payment`). Por eso existen métodos como `withId(...)`: crean una copia con el id asignado.

**`enum` con `DbEnum`.** Las constantes están en inglés (`ACTIVE`), pero `dbValue()` guarda el texto exacto en español que espera MySQL ("Activo").

**Inyección de dependencias sin framework.** Cada servicio recibe sus DAOs, el `TransactionManager` y un `Clock` por el constructor. Solo `ApplicationFactory` crea las clases `Jdbc*`. Esto permite que las pruebas usen otras implementaciones.

**`Clock`.** Los servicios piden la fecha actual a un `Clock` en vez de `LocalDate.now()` directo, para que las pruebas puedan fijar "hoy".

## 7. Principios SOLID y patrones (pregunta casi segura)

| Concepto | Dónde verlo |
|---|---|
| **S** Responsabilidad única | `InputValidator` solo valida; `ConsoleUtils` solo lee/escribe; `AverageCalculator` solo calcula. |
| **O** Abierto/Cerrado | `Menu` + `Menu.Item`: se agregan opciones sin modificar `Menu`. `AverageStrategy`: otra forma de promediar sería otra clase. |
| **L** Sustitución de Liskov | Las pruebas reemplazan `SystemDao` por versiones en memoria y el servicio funciona igual. |
| **I** Segregación de interfaces | `CatalogDao` y `ReportDao` solo tienen métodos de lectura. |
| **D** Inversión de dependencias | Los servicios dependen de interfaces (`PaymentDao`, `TransactionManager`), no de clases JDBC. |
| **Singleton** | `AppConfig`, `DatabaseConnection` (abre una conexión nueva en cada llamada; no comparte una). |
| **Factory** | `ApplicationFactory` construye todo el sistema. |
| **Strategy** | `AverageStrategy` / `ArithmeticMeanStrategy` y `TaxPolicy`. |
| **DAO** | Paquetes `dao` y `dao/jdbc`. |
| **Template** | `inTransaction`: abrir → ejecutar → commit/rollback → cerrar. |

## 8. Archivos clave

| Archivo | Para qué sirve |
|---|---|
| `Main.java` | Arranque y cierre ordenado. |
| `ApplicationFactory.java` | Arma todas las piezas y las conecta. |
| `controller/BaseController.java` | Convierte excepciones en mensajes para el usuario. |
| `dao/TransactionManager.java` + `dao/jdbc/JdbcTransactionManager.java` | Transacciones con commit/rollback. |
| `dao/jdbc/JdbcSupport.java` | Ayudas para ejecutar SQL y leer valores que pueden ser NULL. |
| `service/AcademicService.java` | Inscripciones, notas y promedios. |
| `service/AverageCalculator.java` | Cálculo de promedios con Streams (sin base de datos). |
| `service/PaymentService.java` | Pagos con protección contra duplicados. |
| `service/InvoiceService.java` | Número de factura, CUFE, QR y PDF (y su recuperación si falla). |
| `service/OverdueService.java` | Regla de morosidad. |
| `service/ClubService.java` / `LoanService.java` | Ejemplos de bloqueo con `FOR UPDATE`. |
| `util/InputValidator.java` | Todas las validaciones de texto del usuario. |
| `util/ConsoleUtils.java` | Lectura y escritura en consola (repite la pregunta si el dato es inválido). |
| `view/Menu.java` | Menú reutilizable. |
| `docs/database.sql` | Esquema oficial, cambios autorizados (P-1, P-2) y datos de prueba. |

## 9. Cómo ejecutarlo y probarlo

1. Crear la base: `mysql -u USUARIO -p --default-character-set=utf8mb4 < docs/database.sql`
2. Copiar `config.example.properties` como `config.properties` y poner usuario y contraseña.
3. En NetBeans: *Run Project* (F6). O en terminal: `mvn clean compile exec:java`.
4. Pruebas: `mvn test`. Las de integración usan una base aparte (`erp_little_horizons_test`) que se crea sola; si no hay `config.properties`, se omiten.

## 10. Preguntas de sustentación (con respuesta)

1. **¿Por qué separaste en capas?** Para que cada parte tenga una sola responsabilidad. Si mañana se cambia la consola por una página web, solo cambian las vistas; los servicios y DAOs siguen iguales.
2. **¿Qué es una transacción y dónde la usas?** Un grupo de operaciones que se guardan todas o ninguna. Ejemplo: registrar un pago y su factura; retirar un estudiante y cerrar su matrícula; prestar un recurso y cambiar su estado.
3. **¿Cómo evitas la inyección SQL?** Todas las consultas usan `PreparedStatement` con parámetros `?`; el texto del usuario nunca se concatena al SQL.
4. **¿Cómo evitas que un club supere su cupo si dos personas inscriben a la vez?** La transacción bloquea la fila del club con `SELECT ... FOR UPDATE` y cuenta los miembros activos dentro de la misma transacción.
5. **¿Qué pasa si falla la generación del PDF?** El pago y la factura ya quedaron guardados en la transacción. El PDF se genera después; si falla, la opción "Regenerar facturas PDF pendientes" lo vuelve a crear sin duplicar el pago.
6. **¿Cómo se calcula el promedio de una materia?** Si existe nota Final, es esa; si no, la media de los períodos I a IV con nota, redondeada a un decimal con HALF_UP. Las materias sin notas no cuentan como cero.
7. **¿Qué ve el usuario si MySQL está apagado?** Un mensaje en español que le pide revisar MySQL y `config.properties`. El detalle técnico va a `error_log.txt`.
8. **¿Por qué no se borran estudiantes?** Porque las claves foráneas del esquema borran en cascada (se perdería el historial). En su lugar se cambia el estado a "Retirado".
9. **¿Para qué sirve `ApplicationFactory`?** Construye todos los objetos en un solo lugar y les pasa sus dependencias por constructor (inyección de dependencias sin framework).
10. **¿Por qué usas `record`?** Porque los datos (estudiante, pago, nota) son inmutables: se crean una vez y, si cambian, se crea una copia. Eso evita modificaciones accidentales.
11. **¿El CUFE es real?** No. Es un SHA-384 de número de factura, NIT, fecha y total, que imita la forma de un CUFE. Las facturas son una simulación académica sin validez ante la DIAN.
12. **¿Qué patrón de diseño usaste para el promedio?** Strategy: `AverageStrategy` define cómo combinar notas y `ArithmeticMeanStrategy` es la implementación actual. Un promedio ponderado sería otra clase, sin cambiar los servicios.

## 11. Retos para practicar (hazlos tú)

1. **Fácil:** agrega a `Formats` un método que muestre un porcentaje con el símbolo % y úsalo en el reporte de ocupación de clubes.
2. **Medio:** crea `WeightedMeanStrategy` (implementa `AverageStrategy`) donde el período IV valga el doble, y explica qué tendrías que cambiar en `ApplicationFactory` para usarla.
3. **Medio:** agrega una opción al menú de recursos que liste solo los recursos "En Mantenimiento". Sigue el flujo vista → controlador → servicio → DAO.
4. **Avanzado:** sigue con el depurador de NetBeans (breakpoints) el registro de un pago, desde `FinanceMenuView.registerPayment()` hasta el `INSERT` en `JdbcPaymentDao`, y anota cada clase por la que pasa.
