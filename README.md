# Shooter 2D
Proyecto personal antiguo 

Juego hecho en Java con [libGDX](https://libgdx.com/) (backend de escritorio LWJGL 3), licencia GPL.
Funciona en Windows, macOS y Linux; Gradle descarga todas las dependencias.

Ejecutar: `./gradlew run` (se abre en una ventana de 1600x900, más pequeña si la pantalla no da para tanto).

Controles: 

W A S D       - moverse
Ratón         - apuntar
Clic izquierdo - disparar / construir (con un muro, medio muro o suelo en la mano)
Clic derecho  - usar botiquín / derribar (con un objeto construible en la mano)
1-9, 0, rueda - elegir casilla del inventario (10 casillas)
Q             - soltar el objeto seleccionado
E             - coger un objeto del suelo
N/M           - cambiar el tamaño del HUD
H             - lanzar una horda de zombis
R             - volver al punto de inicio
P, ESPACIO    - pausa (también el botón de arriba a la derecha)
ESCAPE        - salir del juego

Mundos:

Por defecto se carga el "Puesto avanzado" (`OutpostWorld`): casa inicial con armas y materiales,
aldea, fuerte militar, cementerio con horda, ruinas y algún secreto. Con `--classic` se carga el
mapa de pruebas original (`DemoWorld`). Ejemplo: `./gradlew run --args="--classic"`

Texturas:

Las imágenes están en `assets-raw/textures/`. Al compilar, la tarea `packTextures` las junta en un atlas
(`atlas/game.atlas`) que se carga al arrancar. Para añadir una, deja el PNG en esa carpeta y añade su constante
en `TextureId`. Para animarla, pon sus frames como `nombre_0.png`, `nombre_1.png`... (100 ms por frame).

Los suelos y las estructuras están en pixel art (16 px por casilla) en `assets-raw/textures/pixel/`, que se
empaqueta sin suavizado. Son provisionales y los dibuja la tarea `./gradlew generateArt`
(`src/tools/java/tools/PixelArtGenerator.java`); se pueden sustituir por otros PNG con las mismas medidas. En los
suelos, las imágenes numeradas (`3-grass_0.png`, `3-grass_1.png`...) son variantes que se reparten por el mapa.
