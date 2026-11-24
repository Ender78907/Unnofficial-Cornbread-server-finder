# Pruebas y cambios — v0.6.8

Fecha: 2 de noviembre de 2026. Base: entrega v0.6.7 con Server Inspector, construida desde los tres ZIP proporcionados previamente.

## Cambios

Dos casillas persistentes, desactivadas por defecto, controlan el bloqueo y la búsqueda de otro servidor tras un fallo de Search & join, con inclusión opcional de whitelist. El intento se consume una sola vez y se distingue de las entradas manuales. Una conexión exitosa o la cancelación del intento desarma la recuperación. La pantalla de desconexión ofrece 60 ticks (aproximadamente 3 segundos) para cancelar antes de bloquear y buscar. No se cambia de pantalla dentro de init: la recuperación se ejecuta desde tick. Los filtros y las exclusiones se conservan en la búsqueda siguiente. Si no se puede guardar el bloqueo, la búsqueda automática se detiene y muestra el aviso.

La detección de whitelist recorre componentes, argumentos traducibles y hermanos buscando claves de traducción, y usa como respaldo whitelist/white-list/allowlist/lista blanca y variantes comunes en los mensajes. Los mensajes personalizados sin una pista reconocible no se pueden clasificar con certeza.

Se agregaron botones de copia individual y de todos los nombres de la pestaña actual en PlayersScreen. La lista completa incluye todas las páginas, conserva su orden, elimina duplicados sin distinguir mayúsculas y no incluye fechas/UUID. Se eliminan controles y códigos de formato del texto copiado. Se mantienen los avisos sobre muestras online parciales o datos guardados.

## Compilación

Java: Eclipse Temurin OpenJDK 25.0.5+8 local. Fabric Loom 1.17.21. Gradle wrapper: 9.5.1 para 26.2 y 9.6.0 para 26.3. Fabric API: 0.161.0+26.2 y 0.162.0+26.3. Loader de compilación: 0.19.5. Se utilizó el caché local de dependencias, con --offline --no-daemon --console=plain.

Se ejecutó clean build para las casillas y luego build tras integrar los botones de copia y los ajustes de distribución de controles. Las dos compilaciones finales terminaron en BUILD SUCCESSFUL. No se modificó el Java global.

El primer intento de esta sesión no pudo conectar con el proceso local de Gradle; se habilitó la red para esa conexión. Al ejecutar los tests se detectaron restricciones de renombrado atómico en la carpeta temporal del entorno. Se configuró java.io.tmpdir para las pruebas en build/tmp/test-files, sin alterar el guardado del mod. Las pruebas de persistencia pasaron con esa carpeta.

| Minecraft | Versión del mod | Tests | Fallos | Errores | Omitidos |
|---|---|---|---|---|---|
| 26.2 | 0.6.8 | 61 | 0 | 0 | 0 |
| 26.3 | 0.6.8+mc26.3 | 61 | 0 | 0 | 0 |

22 casos nuevos en JoinAttemptTest por proyecto, contando entradas parametrizadas: opciones predeterminadas, configuración antigua, persistencia de preferencias, reintento de un fallo automático sin bloquear antes de tiempo, entradas manuales, siete mensajes de whitelist, whitelist por clave traducida, inclusión opcional, opción secundaria sin la principal, pantalla ajena, inicialización repetida, cancelación/finalización, sustitución por entrada manual, copia del servidor intentado y tres fallos no relacionados con whitelist. Los otros 39 casos existentes también pasaron, incluidos historial, favoritos, gestión, filtros, ubicación, pings locales y Server Inspector.

La compilación final incluye PlayersScreen y la rutina de preparación de nombres para el portapapeles. No se realizaron pruebas del portapapeles del sistema ni de los botones dentro de Minecraft.

Se verificaron los JAR de instalación: metadatos de versión y Minecraft resueltos, clases de Inspector y JoinAttempt, mixins, bytecode major 69 (Java 25). Se comprueba el ZIP final contra el manifiesto SHA-256 incluido.

## Prueba manual pendiente

No se inició Minecraft, no se probaron los mixins en ejecución ni una conexión de juego real. Pendiente en cada versión: activar la primera casilla, usar Search & join contra un servidor que rechace el intento, comprobar el bloqueo y la continuación; repetir con whitelist con la segunda casilla desmarcada/marcada, cancelar la cuenta atrás, probar una entrada manual y una conexión exitosa. En Online/Anteriores, copiar un nombre y todos los nombres, cambiar de página y pegar en un editor para comprobar el contenido. Confirmar preferencias al reiniciar y probar las pantallas a distintas escalas de interfaz.
