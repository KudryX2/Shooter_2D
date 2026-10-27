package graphics;

/**
 * Dibuja un frame. Cada capa la dibuja su clase ({@link WorldRenderer}, {@link CharacterRenderer},
 * {@link EffectsRenderer}, {@link HudRenderer}); aquí solo se decide el orden, de abajo arriba.
 */
public final class GraphicsManager {

    private GraphicsManager() {
    }

    public static void init() {
        HudRenderer.init();
    }

    public static void render() {
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
    }
}
