# Cornbread Finder v0.6.8 — Minecraft 26.2 y 26.3

## Instalar

Elegí JAR/26.2 o JAR/26.3 y copiá el JAR normal, sin -sources, a mods de esa instalación Fabric. Reemplazá el Cornbread Finder anterior. Necesitás Java 25 y Fabric API correspondiente. Loader mínimo declarado: 0.19.3 en 26.2 y 0.19.5 en 26.3.

## Nuevas casillas en el buscador

- **Bloquear y reintentar**: al usar Buscar y entrar / Search & join, si falla el intento de conexión, espera 3 segundos, bloquea ese servidor y busca otro con los mismos filtros. En pantallas angostas aparece como Bloquear y seguir / Block & retry.
- **También por whitelist**: incluye los rechazos por whitelist en ese comportamiento. Requiere activar la primera casilla. En pantallas angostas: También whitelist / Whitelist too.

Ambas vienen desactivadas y se guardan al cerrar el juego. Con la primera activa y la segunda desmarcada, un rechazo por whitelist detiene el proceso sin bloquear. Cancelar reintento durante la cuenta atrás evita tanto el bloqueo como la búsqueda siguiente. Volver a otra pantalla también detiene la cuenta.

La función se aplica a conexiones iniciadas automáticamente por Search & join y Buscar otro; una entrada manual no activa el reintento. Entrar correctamente termina ese intento: una desconexión posterior de la partida no dispara esta función. Los servidores bloqueados se recuperan desde Bloqueados.

## Copiar nombres de jugadores

Desde … → Jugadores online o Jugadores anteriores:

- **Copiar**, junto a cada jugador: copia solamente ese nombre.
- **Copiar todos**, al pie: copia los nombres de la pestaña abierta en todas sus páginas, uno por línea y sin repetir. En pantallas angostas este botón se llama Copiar y su ayuda indica el alcance.

No se copian fechas ni UUID. La copia usa los nombres disponibles en esa lista. Online puede ser una muestra parcial o información guardada si no respondió el servidor; Anteriores combina el historial disponible con observaciones locales. Los avisos de la pantalla siguen indicando ese estado.

## Otros contenidos

Server Inspector y las funciones previas se conservan. Fuentes/ contiene ambos proyectos con wrapper y pruebas; Pruebas/ contiene registros de compilación, XML y reportes HTML. Los JAR -sources son para desarrollo. Las imágenes heredadas son maquetas o guías ilustrativas, no capturas de esta versión ejecutándose. SHA256.csv permite comprobar integridad de los archivos.

La búsqueda sigue usando la API de Cornbread. Si falla la API durante la búsqueda (por ejemplo HTTP 523), no se bloquea ningún servidor por ese fallo: todavía no se inició una conexión Minecraft.
