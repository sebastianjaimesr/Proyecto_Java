USE erp_little_horizons;

INSERT IGNORE INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion)
SELECT 1, id_materia, 2025, 'A' FROM materias WHERE codigo_materia = 'MAT-10';

INSERT IGNORE INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion)
SELECT 1, id_materia, 2025, 'A' FROM materias WHERE codigo_materia = 'MAT-11';

INSERT IGNORE INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion)
SELECT 1, id_materia, 2025, 'A' FROM materias WHERE codigo_materia = 'CNA-10';

INSERT IGNORE INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion)
SELECT 1, id_materia, 2025, 'B' FROM materias WHERE codigo_materia = 'MAT-10';

SELECT ap.id_profesor, ma.codigo_materia, ma.nombre_materia, g.nombre_grado, ap.seccion
FROM asignaciones_profesor ap
JOIN materias ma ON ma.id_materia = ap.id_materia
JOIN grados g ON g.id_grado = ma.id_grado
WHERE ap.año_academico = 2025
ORDER BY ap.id_asignacion;
