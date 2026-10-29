package graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import assets.TextureId;
import input.Input;
import player.Player;

/** Inventario, botón de pausa y cursor, en coordenadas de pantalla. */
public final class HudRenderer {
    /** Tamaño de casilla de partida (más {@link #hudScale}) en píxeles de pantalla. */
    private static final int BASE_CELL_SIZE = 50;
    /** Lado de la imagen de una casilla, en píxeles del dibujo. */
    private static final float CELL_PIXELS = 16;
    private static final float CURSOR_SIZE = 26;

    /** Escala del HUD (teclas N/M). */
    public static float hudScale;

    private HudRenderer() {
    }

    static void init() {
        hudScale = DisplayManager.WIDTH / 100f;
    }

    /**
     * Barra del inventario centrada abajo. Las casillas (16x16 px de pixel art) se amplían un número entero de veces
     * según {@link #hudScale}, y los iconos una vez menos, para que quepan dentro con margen: así todos los píxeles
     * salen iguales.
     */
    static void draw() {
        float width = DisplayManager.WIDTH;
        float height = DisplayManager.HEIGHT;
        int pixel = Math.max(1, Math.round((BASE_CELL_SIZE + hudScale) / CELL_PIXELS));
        int iconPixel = Math.max(1, pixel - 1);
        float cellSize = CELL_PIXELS * pixel;
        float step = cellSize + pixel; // un píxel de separación
        float cellY = Math.round(height / 1.1f);
        float firstX = Math.round((width - step * Player.INVENTORY_SLOTS) / 2);

        for (int i = 0; i < Player.INVENTORY_SLOTS; i++) {
            float x = firstX + step * i;
            Draw.sprite(TextureId.NORMAL_CELL, x, cellY, cellSize, cellSize);
            if (i < Player.inventory.size()) {
                TextureRegion icon = TexturesManager.region(Player.inventory.get(i).icon);
                float iconWidth = icon.getRegionWidth() * iconPixel, iconHeight = icon.getRegionHeight() * iconPixel;
                float iconX = x + Math.round((cellSize - iconWidth) / 2);
                float iconY = cellY + Math.round((cellSize - iconHeight) / 2);
                Draw.region(icon, iconX, iconY, iconWidth, iconHeight, 0);
            }
            if (i == Player.inventory.selectedSlot()) {
                Draw.sprite(TextureId.SELECTED_CELL, x, cellY, cellSize, cellSize);
            }
        }

        PauseButton.draw();

        // Cursor (siempre lo último, por encima de todo el HUD). Con un objeto construible, la vista previa en el
        // mundo ya indica si se puede construir.
        // Pixel art de 13x13 a 2 px por píxel: más o menos del tamaño de una cabeza en pantalla (~22 px), para poder
        // apuntar a ella. Tamaño fijo (no crece con el HUD) para que los píxeles salgan parejos.
        float cursorSize = CURSOR_SIZE;
        Draw.sprite(TextureId.POINTER, Input.mouseX() - cursorSize / 2, Input.mouseY() - cursorSize / 2,
                cursorSize, cursorSize);
    }
}
