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
