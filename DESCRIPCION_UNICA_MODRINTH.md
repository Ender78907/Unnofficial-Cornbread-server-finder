# Cornbread Finder — v0.6.8 (Minecraft 26.2 y 26.3)

Buscá servidores y encontrá dónde jugar desde Multijugador, con interfaz en español e inglés.

- Búsqueda con filtros de jugadores, whitelist, cracked y protocolo compatible, verificación paralela y Search & join para entrar al primer resultado verificado.
- **Nuevo: bloqueo y búsqueda automática tras errores al entrar**, con casilla separada para incluir rechazos por whitelist y cuenta atrás cancelable de 3 segundos. Las preferencias se conservan al reiniciar.
- **Nuevo: copiar nombres de jugadores online y anteriores**, individualmente o toda la lista de la pestaña, uno por línea y sin duplicados.
- Favoritos, alias, historial de conexiones e intentos fallidos, bloqueos y servidores guardados en Multijugador.
- Jugadores publicados por el servidor, historial disponible y ubicación aproximada de IP.
- Menú dentro del servidor para salir y buscar otro, bloquear, favoritos y alias.
- **Server Inspector**: consulta el estado público del servidor y muestra versión, protocolo, indicios de software, loader, proxy y mods anunciados cuando se publican.

## Compatibilidad y límites

Elegí el archivo para Minecraft 26.2 o 26.3, con Fabric Loader, Fabric API y Java 25. Reemplazá la versión anterior del mod.

Las listas de jugadores pueden ser parciales, personalizadas o datos guardados; el mod informa su estado. La detección de whitelist usa la clave traducible de Minecraft y mensajes habituales en inglés/español. Un mensaje personalizado que no mencione whitelist puede no reconocerse como tal.

El inspector muestra indicios del ping público. No garantiza distinguir Paper de Vanilla, detectar mods ocultos, verificar whitelist/online-mode ni resolver incompatibilidades como moonlight:map_marker. La búsqueda requiere que la API de Cornbread esté disponible.

Ambas versiones fueron compiladas y pasaron 61 pruebas automatizadas por proyecto. No se verificaron estas funciones dentro de una partida; las imágenes incluidas son ilustrativas.
