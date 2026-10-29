package graphics;

import assets.Assets;

/**
 * Dibuja un frame. Cada capa la dibuja su clase ({@link WorldRenderer}, {@link CharacterRenderer},
 * {@link DepthRenderer}, {@link EffectsRenderer}, {@link HudRenderer}); aquí solo se decide el orden, de abajo
 * arriba.
 */
public final class GraphicsManager {

    private GraphicsManager() {
    }

    /** Requiere la ventana creada. Lo justo para la pantalla de carga: aún no hay texturas. */
    public static void init() {
        Draw.init();
    }

    /** Cuando {@link assets.Assets} ha terminado de cargar. */
    public static void onAssetsLoaded() {
        TexturesManager.init();
        HudRenderer.init();
    }

    /** Pantalla de carga: barra de progreso ({@code progress} de 0 a 1) en el centro. */
    public static void renderLoading(float progress) {
        Draw.begin();
        Draw.hudView();
        LoadingRenderer.draw(progress);
        Draw.end();
    }

    public static void resize(int width, int height) {
        DisplayManager.resize(width, height);
        Draw.resize(width, height);
    }

    public static void render() {
        Draw.begin();
        // Suelo: todo lo plano
        WorldRenderer.drawFloors();
        WorldRenderer.drawWallShadows();
        WorldRenderer.drawDroppedItems();
        CharacterRenderer.drawGround();
        EffectsRenderer.drawProjectiles(); // bajo los muros: una bala que choca no asoma por encima
        // Lo que tiene altura, ordenado de norte a sur
        DepthRenderer.draw();
        // Encima de todo el mundo
        WorldRenderer.drawBuildPreview();
        EffectsRenderer.drawMuzzleFlashes();
        CharacterRenderer.drawHealthBars();
        Draw.hudView();
        HudRenderer.draw();
        Draw.end();
    }

    public static void dispose() {
        Draw.dispose();
        TexturesManager.dispose();
        Assets.dispose();
    }
}
