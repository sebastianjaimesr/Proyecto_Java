# 1. ERP Colegio Internacional Little Horizons S.A.S.

Sistema de gestión educativa de consola desarrollado en Java 17 con MySQL 8, JDBC y arquitectura MVC con capas DAO y Service.

---

## 2. Descripción

### Problema

El Colegio Internacional Little Horizons (preescolar a grado 11.º) necesita un sistema que reúna en un solo lugar la información académica, extracurricular, financiera y de recursos, que hoy estaría dispersa:

- Estudiantes, acudientes (con hermanos que comparten acudiente), matrículas por año y grado.
- Profesores, materias por grado, asignaciones, horarios y conflictos de horario.
- Inscripción en materias, calificaciones de 0.0 a 5.0 por período, promedios e historial académico.
- Asistencia diaria y alertas de inasistencia.
- Clubes con cupo limitado, eventos y resultados de competencias.
- Conceptos de pago, becas y descuentos, pagos, morosidad y facturas.
- Inventario de recursos y préstamos a estudiantes y profesores.

### Solución

Una aplicación de consola en español, organizada en siete módulos y un menú de configuración, que trabaja sobre el esquema oficial `erp_little_horizons`. Las reglas de negocio están en la capa de servicios, todas las consultas usan `PreparedStatement` y las operaciones que modifican varias tablas se ejecutan en transacciones con *rollback*.

---

## 3. Autores

| Nombre | Perfil | Correo | GitHub |
|---|---|---|---|
| Sebastian Jaimes | Desarrollador Full Stack en formación | [jsrolon01@gmail.com](mailto:jsrolon01@gmail.com) | [github.com/sebastianjaimesr](https://github.com/sebastianjaimesr) |

---

## 4. Tecnologías utilizadas

Versiones verificadas en el equipo de desarrollo (Windows 11):

| Tecnología | Versión | Uso |
|---|---|---|
| Java (JDK) | 17.0.12 LTS | Lenguaje; el proyecto compila con `release 17` |
| Apache NetBeans | Incluido en el equipo (perfil 31) | IDE del proyecto |
| Apache Maven | 3.9.16 (incluido en NetBeans) | Construcción y dependencias |
| MySQL Server | 8.0.46 Community | Base de datos |
| MySQL Connector/J | 8.4.0 | Driver JDBC (línea LTS para MySQL 8.0/8.4) |
| OpenPDF | 2.0.5 | Generación de facturas PDF |
| ZXing core / javase | 3.5.3 | Código QR de la factura |
| JUnit Jupiter | 5.14.4 | Pruebas automatizadas (solo en `test`) |
| `java.util.logging` | JDK | Registro de errores en `error_log.txt` |

No se usan otras librerías.

---

## 5. Requerimientos previos

- JDK 17 o superior.
- MySQL Server 8.0 o superior en ejecución (los `CHECK` del esquema requieren MySQL 8).
- Apache NetBeans con soporte Java y Maven (o Maven 3.9+ en la línea de comandos).
- Un usuario de MySQL con permisos para crear la base `erp_little_horizons`.
- Conexión a Internet la primera vez que se compila, para que Maven descargue las dependencias.

---

## 6. Configuración de la base de datos

El archivo [`docs/database.sql`](docs/database.sql) contiene:

| Sección | Contenido |
|---|---|
| 1 | DDL oficial del instructor, copiado sin cambios, con sus datos iniciales: 12 grados, 5 categorías de recursos y 7 conceptos de pago. |
| 1-B | Dos tablas nuevas autorizadas: `horarios_clase` (P-1) y `valores_estudiante_concepto` (P-2). No se modificó ninguna tabla oficial. |
| 2 | Datos de prueba ficticios: 60 estudiantes, 78 acudientes, 40 profesores, 48 materias, 932 notas, 634 pagos, 30 clubes, 37 recursos, además de matrículas, asistencia, eventos y préstamos. |
| 3 | Consultas que verifican los conteos de las tablas principales. |

> **Advertencia:** la sección 1 empieza con `DROP DATABASE IF EXISTS erp_little_horizons`. Ejecutar el archivo borra esa base y la crea de nuevo.

### Crear la base

Con la consola de MySQL (desde la carpeta del proyecto):

```
mysql -u USUARIO -p --default-character-set=utf8mb4 < docs\database.sql
```

También se puede abrir `docs/database.sql` en MySQL Workbench o DBeaver y ejecutarlo completo.

Conteos verificados después de cargar el archivo (sección 3):

| Tabla | Registros |
|---|---|
| estudiantes | 60 |
| acudientes | 78 |
| profesores | 40 |
| materias | 48 |
| registros_academicos | 932 |
| pagos | 634 |
| clubes | 30 |
| recursos | 37 |

### Cambios autorizados al esquema

| Cambio | Motivo |
|---|---|
| P-1 `horarios_clase` (día, hora de inicio y fin, aula por asignación) | El esquema oficial no permite registrar días y horas de clase ni detectar conflictos de horario. |
| P-2 `valores_estudiante_concepto` (valor personalizado, beca o descuento por hermanos, por año) | El esquema oficial no permite guardar valores por estudiante. |

Se evaluó y **no** se creó una tabla de cuentas por cobrar: la morosidad se infiere de los pagos (ver sección 13).

---

## 7. Instalación y ejecución

### 7.1 Clonar el repositorio

```
git clone https://github.com/sebastianjaimesr/Proyecto_Java.git
cd Proyecto_Java
```

### 7.2 Configurar las credenciales

1. Copiar `config.example.properties` como `config.properties` en la carpeta raíz del proyecto.
2. Completar `db.user` y `db.password` con un usuario real de MySQL.
3. Opcional: completar los datos legales del emisor (`issuer.nit`, `issuer.address`, `issuer.phone`, `issuer.email`). Si se dejan vacíos, la factura muestra "PENDIENTE DE CONFIGURACIÓN".

`config.properties` está en `.gitignore` y nunca debe subirse al repositorio. La contraseña no se muestra en pantalla ni se escribe en los registros.

| Propiedad | Ejemplo | Descripción |
|---|---|---|
| `db.host`, `db.port`, `db.name` | `localhost`, `3306`, `erp_little_horizons` | Servidor y base |
| `db.user`, `db.password` | (propios) | Credenciales |
| `db.params` | `serverTimezone=America/Bogota&characterEncoding=UTF-8&connectTimeout=5000` | Parámetros JDBC |
| `logs.dir` | `logs` | Carpeta de `error_log.txt` |
| `console.encoding` | `AUTO` | Codificación de la consola |
| `invoices.dir` | `invoices` | Carpeta de las facturas PDF |
| `issuer.*` | vacío | Datos del colegio en la factura |
| `finance.tax.percent.CODIGO` | `finance.tax.percent.UNIFORME=19` | IVA por concepto (sin línea, no aplica) |
| `finance.mandatory.concepts` | `MAT,PENSION` | Conceptos que todo matriculado debe pagar |

### 7.3 Ejecutar desde NetBeans

1. *File → Open Project* y elegir la carpeta `Proyecto_Java` (NetBeans la reconoce como proyecto Maven por el `pom.xml`).
2. *Run → Run Project* (F6). La configuración de ejecución está en `nbactions.xml` (clase `com.littlehorizons.erp.Main`).
3. La aplicación se ejecuta en la ventana *Output*, donde se escriben las opciones.

### 7.4 Ejecutar desde la línea de comandos

Con el Maven incluido en NetBeans (Windows):

```
"C:\Program Files\Apache NetBeans\java\maven\bin\mvn" clean compile exec:java
```

En la consola de Windows (`cmd`) los acentos se muestran bien con la opción `console.encoding=AUTO`, que usa la página de códigos de la terminal.

---

## 8. Estructura del proyecto

```
Proyecto_Java/
├── pom.xml                       Construcción Maven (Java 17, dependencias)
├── nbactions.xml                 Acciones Run/Debug de NetBeans
├── config.example.properties     Plantilla de configuración (sin datos reales)
├── docs/database.sql             DDL oficial + cambios autorizados + datos de prueba
├── docs/plan-de-pruebas.md       Pruebas con objetivo, datos, resultado esperado y observado
├── src/main/java/com/littlehorizons/erp/
│   ├── Main.java                 Punto de entrada
│   ├── ApplicationFactory.java   Construye DAOs, servicios, controladores y vistas
│   ├── model/                    Entidades (records) y enums por módulo
│   │   ├── academic/ extracurricular/ finance/ resources/ report/ system/ common/
│   ├── dao/                      Interfaces DAO y TransactionManager
│   │   └── jdbc/                 Implementaciones JDBC
│   ├── service/                  Reglas de negocio, cálculos y transacciones
│   ├── controller/               Coordinación vista-servicio y OperationResult
│   ├── view/                     Menús y formularios de consola
│   ├── util/                     Configuración, conexión, logger, validación, consola, PDF, CUFE, QR
│   └── exception/                Excepciones propias
├── src/test/java/...             Pruebas JUnit (unitarias e integración)
├── logs/error_log.txt            (generado; excluido de Git)
└── invoices/factura_NNNNNN.pdf   (generado; excluido de Git)
```

### Capas MVC

El flujo es siempre **View → Controller → Service → DAO → MySQL**:

| Capa | Responsabilidad | Ejemplo |
|---|---|---|
| View | Menús, lectura y validación inicial de entradas, tablas en español | `FinanceMenuView.registerPayment()` |
| Controller | Llama al servicio y convierte excepciones en `OperationResult` con mensaje en español | `FinanceController.registerPayment()` |
| Service | Reglas de negocio y transacciones | `PaymentService.register()` |
| DAO | SQL parametrizado y mapeo de filas a objetos | `JdbcPaymentDao.insert()` |

Ninguna vista usa DAOs, ningún controlador escribe en la consola y solo los DAO contienen SQL.

---

## 9. Principios y patrones de diseño aplicados

### Principios SOLID con ejemplos del código

| Principio | Dónde | Explicación |
|---|---|---|
| **S**ingle Responsibility | `InputValidator`, `ConsoleUtils`, `AverageCalculator`, `InvoicePdfGenerator` | `InputValidator` solo valida texto (no lee la consola). `ConsoleUtils` solo lee y escribe. `AverageCalculator` solo calcula promedios, sin base de datos. `InvoicePdfGenerator` solo dibuja el PDF a partir de un `InvoiceDocument` ya calculado. |
| **O**pen/Closed | `Menu` + `Menu.Item`, `AverageStrategy`, `TaxPolicy` | Un módulo nuevo se agrega con un `Menu.Item` sin modificar `Menu`. Una regla de promedio ponderado sería otra implementación de `AverageStrategy`, sin cambiar `AcademicService`. |
| **L**iskov Substitution | `ConnectionProvider`, `TransactionManager`, interfaces DAO | Las pruebas sustituyen `SystemDao` por implementaciones en memoria (`SystemServiceTest`) y `DatabaseConnection` por otra configuración (`TestDatabase`); los servicios funcionan igual con cualquiera. |
| **I**nterface Segregation | `CatalogDao`, `ReportDao`, `SystemDao` | Los catálogos de solo lectura (grados) y los reportes tienen interfaces sin métodos de escritura; cada DAO expone solo lo que su tabla necesita. |
| **D**ependency Inversion | Constructores de todos los servicios | `PaymentService` depende de `PaymentDao`, `TransactionManager` y `Clock` (abstracciones), recibidos por constructor. Solo `ApplicationFactory` conoce las clases `Jdbc*`. |

### Patrones

| Patrón | Dónde | Problema que resuelve |
|---|---|---|
| MVC | Paquetes `view`, `controller`, `model` | Separa la consola de las reglas y de los datos. |
| DAO | `dao` y `dao.jdbc` | Los servicios no conocen SQL; las pruebas pueden cambiar la persistencia. |
| Singleton | `AppConfig`, `DatabaseConnection` | La configuración se lee una vez. `DatabaseConnection` es un **proveedor**: cada llamada abre una conexión nueva, así que nunca se comparte una conexión ni una transacción entre operaciones. |
| Factory | `ApplicationFactory` | Construye todo el grafo de objetos en un solo lugar (inyección por constructor sin framework). |
| Strategy | `AverageStrategy` / `ArithmeticMeanStrategy`, `TaxPolicy` | Algoritmos intercambiables: regla de promedio y regla de IVA. |
| Template (transacción) | `JdbcTransactionManager.inTransaction` | Abre la conexión, hace *commit* o *rollback* y cierra siempre, con try-with-resources. |

### Lambdas y Stream API

- Promedios por materia, generales y del curso (`AverageCalculator`): `groupingBy`, `reduce`, `max`.
- Ranking de promedios (`ReportService.ranking`) y resumen de pagos por concepto, método y mes (`groupingBy` + `reducing`).
- Detección de tablas faltantes (`SystemService`), conflictos de horario (`TeacherService.addSchedule`), estado de cuenta y morosidad.
- Los conteos sobre tablas completas se hacen en SQL (`JdbcReportDao`) para no cargar toda la base en memoria.

---

## 10. Funcionalidades principales

Todas las opciones del menú están implementadas.

```
1. Gestión de estudiantes y acudientes
2. Gestión académica
3. Gestión de profesores
4. Actividades extracurriculares
5. Gestión financiera
6. Gestión de recursos
7. Reportes y estadísticas
8. Configuración y estado del sistema
9. Salir
```

| Módulo | Funcionalidades |
|---|---|
| Estudiantes y acudientes | Registro, ficha (los datos médicos se muestran solo si se confirma), actualización, cambio de estado y retiro (también cierra la matrícula activa, en una transacción). Acudientes N:M con principal y contacto de emergencia, detección de hermanos, matrícula por año y grado, cierre de matrícula. |
| Académico | Materias por grado; inscripción individual o en todas las materias del grado; notas I a IV de 0.0 a 5.0 con un decimal; nota final calculada; consultas por estudiante, materia, grupo y período; promedios por materia, general y del curso; historial desde el ingreso; asistencia individual o por grupo; reporte semanal de más de 3 ausencias. |
| Profesores | Registro y consulta, estados, asignación de materia y sección, horario semanal con detección de conflictos del profesor y del grupo. |
| Extracurricular | Clubes con cupo seguro ante inscripciones simultáneas, retiro con historial; eventos por tipo, participantes, posiciones (1er, 2do y 3er lugar, Participante) e historial por estudiante. |
| Financiero | Catálogo de conceptos; valores personalizados, becas y descuentos por hermanos; pagos con recibo único y protección contra registros duplicados; factura PDF con CUFE y QR simulados; historial por estudiante y acudiente; estado de cuenta (pago parcial o completo); obligaciones vencidas; regeneración de PDF pendientes. |
| Recursos | Inventario con código único y estados; préstamos a estudiantes y profesores en sus tablas separadas; devolución atómica; préstamos vencidos. |
| Reportes | 14 reportes: matriculados por grado, promedios, ranking, historial, inasistencias por período, clubes y eventos, competencias, pagos, resumen de recaudo, morosidad, recursos, préstamos vencidos y estadísticas generales. |
| Sistema | Prueba de conexión, verificación de las 22 tablas oficiales y configuración activa (sin mostrar la contraseña). |

### Reglas de negocio documentadas

| Tema | Regla |
|---|---|
| Promedio de materia | La nota Final si existe; si no, la media simple de los períodos I a IV con nota, redondeada a un decimal (HALF_UP). |
| Nota Final | Se calcula (no se digita) como la media simple de I a IV. |
| Promedio general | Media de los promedios de materia. Las materias sin notas aparecen como "Sin calificaciones" y no cuentan como cero. |
| Corrección de notas o asistencia | Reemplazar un dato existente requiere confirmación explícita. |
| Inasistencias | Semana ISO de lunes a domingo; solo "Ausente" cuenta; más de 3 en la semana sumando todas las materias. |
| Cupo de clubes | Se bloquea la fila del club (`SELECT ... FOR UPDATE`) y se cuentan las membresías activas dentro de la misma transacción. |
| Posiciones | Cada posición de podio se asigna a un solo estudiante por evento y solo en eventos con competencia. |
| Valor a pagar | Valor personalizado o valor base, menos la suma de beca y descuento por hermanos (máximo 100 %). |
| Pagos duplicados | Un recibo digitado debe ser único; un pago idéntico (estudiante, concepto, monto, fecha y método) pide confirmación. |
| Préstamos | Solo recursos "Disponible"; máximo 90 días; préstamo y devolución actualizan el recurso en la misma transacción. |
| Eliminación | Estudiantes, profesores, pagos y facturas no se borran (las claves foráneas del esquema borran en cascada); se cambian de estado. |

---

## 11. Manejo de errores y logs

- Los errores técnicos (SQL, conexión, archivos) se registran en **`logs/error_log.txt`** (carpeta configurable con `logs.dir`), con `java.util.logging`, en UTF-8 y en modo anexar.
- Cada entrada incluye fecha y hora, nivel, clase y método, mensaje y *stack trace* completo:

```
[2026-10-15 14:48:27] SEVERE com.littlehorizons.erp.controller.SystemController#execute
Message: Could not open a connection to jdbc:mysql://127.0.0.1:1/erp_little_horizons?connectTimeout=2000 as user 'demo' (SQLState 08S01)
com.littlehorizons.erp.exception.DataAccessException: ...
	at ...
----------------------------------------------------------------------
```

- El usuario ve un mensaje comprensible en español, sin SQL ni *stack traces*, por ejemplo: `[ERROR] No fue posible comunicarse con la base de datos...`.
- Excepciones propias: `ValidationException` (entrada inválida), `BusinessRuleException` (regla incumplida), `ConfirmationRequiredException` (requiere confirmar), `DataAccessException` (error técnico), `ConfigurationException` e `InputUnavailableException`.
- Las violaciones de restricciones de MySQL (duplicados 1062, claves foráneas 1451/1452, CHECK 3819) se traducen a mensajes de negocio.
- No hay bloques `catch` vacíos y nunca se registran contraseñas.

---

## 12. Capturas de pantalla

Salida real de la consola:

```
╔══════════════════════════════════════════════╗
║         ERP COLEGIO LITTLE HORIZONS          ║
║           Iniciando el sistema...            ║
╚══════════════════════════════════════════════╝
[OK] Conectado a MySQL 8.0.46 · base de datos erp_little_horizons.

╔══════════════════════════════════════════════╗
║         ERP COLEGIO LITTLE HORIZONS          ║
║         Sistema de Gestión Educativa         ║
╚══════════════════════════════════════════════╝
1. Gestión de estudiantes y acudientes
2. Gestión académica
3. Gestión de profesores
4. Actividades extracurriculares
5. Gestión financiera
6. Gestión de recursos
7. Reportes y estadísticas
8. Configuración y estado del sistema
9. Salir
```

---

## 13. Problemas conocidos y limitaciones

- **Simulación DIAN:** el CUFE, el código QR y la resolución de facturación son **simulados**. Los documentos generados son representaciones gráficas de demostración: **no son facturas electrónicas validadas ni transmitidas a la DIAN y no tienen validez fiscal**. El sistema no implementa la habilitación ni la transmisión real.
- **CUFE simulado:** SHA-384 (96 caracteres hexadecimales) de `numero_factura|NIT|fecha y hora ISO|total con 2 decimales`, por ejemplo `FE-000123|PENDIENTE DE CONFIGURACIÓN|2026-10-15T14:30:00|350000.00`. La fecha y hora quedan guardadas en `facturas.fecha_creacion`, por lo que el CUFE se puede recalcular.
- **Una línea por factura:** el esquema no tiene tabla de detalle; cada factura corresponde a un pago y a un concepto (cantidad 1). El IVA solo se calcula para los conceptos configurados; el proyecto no decide qué servicios educativos están gravados.
- **Recuperación de PDF:** el pago y la factura se guardan en una transacción; el PDF se genera después, fuera de la transacción. Si falla, el pago no se pierde ni se duplica y la opción "Regenerar facturas PDF pendientes" lo vuelve a crear.
- **Morosidad inferida:** el esquema no tiene cuentas por cobrar y `pagos.periodo_academico` es I a IV o Anual (no mes). Por eso un concepto mensual está vencido cuando su último pago (o la matrícula, si nunca se pagó) tiene más de un mes. La matrícula está vencida cuando lo pagado es menor que lo esperado un mes después de matricularse.
- **Pago parcial:** se compara lo pagado con el valor esperado, por mes en los conceptos recurrentes y por año en los demás.
- **Fechas de los datos de prueba:** simulan 2026 con corte en octubre; los reportes de morosidad y de préstamos vencidos dependen de la fecha en que se ejecute el programa.
- **Datos de prueba sin facturas:** los 634 pagos históricos no tienen factura; las facturas se generan al registrar pagos nuevos desde la aplicación.
- **Documentos de preescolar:** el esquema solo admite TI, CC, CE y Pasaporte; los estudiantes de preescolar se registran con TI.
- **Secciones:** la sección (A, B...) es texto libre en el esquema; la aplicación la guarda en mayúsculas. La restricción única de asignaciones no impide duplicados con sección vacía en MySQL; el servicio sí lo impide.
- **Consola de NetBeans:** la salida se verificó en la terminal y en pruebas automatizadas; si la ventana Output muestra mal los acentos, se puede fijar `console.encoding=UTF-8`.

---

## 14. Futuras mejoras

- Tabla de cuentas por cobrar con obligaciones mensuales para una morosidad exacta.
- Facturas con varias líneas y nota crédito.
- Integración real con un proveedor tecnológico autorizado por la DIAN.
- Autenticación de usuarios y roles (secretaría, docentes, tesorería).
- Exportación de reportes a PDF o CSV.
- Interfaz gráfica o web sobre la misma capa de servicios.

---

## 15. Licencia

No se ha definido una licencia para este proyecto. Es un trabajo académico; su uso, copia o distribución requiere autorización del autor.

---

## 16. Contacto

- Sebastian Jaimes · [jsrolon01@gmail.com](mailto:jsrolon01@gmail.com)
- GitHub: [https://github.com/sebastianjaimesr](https://github.com/sebastianjaimesr)

---

### Anexo: ejecutar las pruebas

Desde NetBeans: clic derecho en el proyecto → *Test* (Alt+F6).

Desde la línea de comandos:

```
"C:\Program Files\Apache NetBeans\java\maven\bin\mvn" clean test
```

- Las pruebas unitarias no necesitan base de datos.
- Las pruebas de integración crean y borran su propia base `erp_little_horizons_test` a partir de `docs/database.sql`; nunca modifican `erp_little_horizons`. Si no existe `config.properties`, se omiten.
- Resultado verificado el 2026-10-18: **114 pruebas, 0 fallos, 0 errores, 0 omitidas**. El detalle de cada prueba está en [`docs/plan-de-pruebas.md`](docs/plan-de-pruebas.md).
