package graphics;

/**
 * Dibuja un frame. Cada capa la dibuja su clase ({@link WorldRenderer}, {@link CharacterRenderer},
 * {@link EffectsRenderer}, {@link HudRenderer}); aquí solo se decide el orden, de abajo arriba.
 */
public final class GraphicsManager {

    private GraphicsManager() {
    }

    /** Requiere la ventana creada. */
    public static void init() {
        TexturesManager.init();
        Draw.init();
        HudRenderer.init();
    }

    public static void resize(int width, int height) {
        DisplayManager.resize(width, height);
        Draw.resize(width, height);
    }

    public static void render() {
        Draw.begin();
        WorldRenderer.drawFloors();
        WorldRenderer.drawWallShadows();
        EffectsRenderer.drawProjectiles(); // bajo los muros: una bala que choca no asoma por encima
        WorldRenderer.drawWalls();
        WorldRenderer.drawBuildPreview();
        WorldRenderer.drawDroppedItems();
        CharacterRenderer.drawNpcs();
        CharacterRenderer.drawPlayer();
        EffectsRenderer.drawMuzzleFlashes();
        HudRenderer.draw();
        Draw.end();
    }

    public static void dispose() {
        Draw.dispose();
        TexturesManager.dispose();
    }
}
