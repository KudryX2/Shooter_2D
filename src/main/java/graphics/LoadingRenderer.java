package graphics;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Pantalla de carga: solo formas, porque las texturas aún no están cargadas. */
final class LoadingRenderer {
    private static final float BAR_WIDTH = 400;
    private static final float BAR_HEIGHT = 16;
    private static final float BORDER = 2;

    private LoadingRenderer() {
    }

    /** Barra de progreso ({@code progress} de 0 a 1) centrada, en coordenadas de pantalla. */
    static void draw(float progress) {
        float x = (DisplayManager.WIDTH - BAR_WIDTH) / 2;
        float y = (DisplayManager.HEIGHT - BAR_HEIGHT) / 2;
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0.35f, 0.35f, 0.35f, 1);
        shapes.rect(x - BORDER, y - BORDER, BAR_WIDTH + BORDER * 2, BAR_HEIGHT + BORDER * 2);
        shapes.setColor(0.1f, 0.1f, 0.1f, 1);
        shapes.rect(x, y, BAR_WIDTH, BAR_HEIGHT);
        shapes.setColor(0.55f, 0.75f, 0.3f, 1);
        shapes.rect(x, y, BAR_WIDTH * Math.clamp(progress, 0f, 1f), BAR_HEIGHT);
    }
}
