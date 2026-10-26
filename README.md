# Shooter 2D
Proyecto personal antiguo 

Juego hecho con java y eclipse, licencia GPL , se puede importar en clipse y ejecutar sin problema

Controles: 

W A S D - MOVEMENT
Q       - drop something
E       - pick up something
Mouse0  - attack
Mouse1  - build/use
N/M     - change hud size
P       - spawn zombies
R       - teleport to spawn
B       - build mode
ESCAPE  - exit the game	
		
Utiliza: https://www.lwjgl.org/ (las dependencias estan en el proyecto)

Mundos:

Por defecto se carga el "Puesto avanzado" (`OutpostWorld`): casa inicial con armas y materiales,
aldea, fuerte militar, cementerio con horda, ruinas y algún secreto. Con `--classic` se carga el
mapa de pruebas original (`DemoWorld`). Ejemplo: `./gradlew run --args="--windowed --classic"`
