package graphics;

import assets.TextureId;
import input.Input;
import player.Player;

/** Inventario, botón de pausa y cursor, en coordenadas de pantalla. */
public final class HudRenderer {
    private static final int CELL_SPACING = 50;

    /** Escala del HUD (teclas N/M). */
    public static float hudScale;

    private HudRenderer() {
    }

    static void init() {
        hudScale = DisplayManager.WIDTH / 100f;
    }

    static void draw() {
        float width = DisplayManager.WIDTH;
        float height = DisplayManager.HEIGHT;
        float cellY = height / 1.1f;
        float cellSize = 50 + hudScale;
        float step = CELL_SPACING + hudScale;
        float firstX = (width - step * Player.INVENTORY_SLOTS) / 2; // barra centrada

        // Objetos del inventario
        for (int i = 0; i < Player.inventory.size(); i++) {
            float x = firstX + 5 + step * i;
            Draw.sprite(Player.inventory.get(i).icon, x, cellY + 5, 40 + hudScale, 40 + hudScale);
        }

        // Casillas
        for (int i = 0; i < Player.INVENTORY_SLOTS; i++) {
            float x = firstX + step * i;
            Draw.sprite(TextureId.NORMAL_CELL, x, cellY, cellSize, cellSize);
            if (i == Player.inventory.selectedSlot()) {
                Draw.sprite(TextureId.SELECTED_CELL, x, cellY, cellSize, cellSize);
            }
        }

        PauseButton.draw();

        // Cursor (siempre lo último, por encima de todo el HUD). Con un objeto construible, la vista previa en el
        // mundo ya indica si se puede construir.
        // Del tamaño de una cabeza en pantalla (~22 px), para poder apuntar a ella; crece poco con el HUD
        float cursorSize = 16 + hudScale / 2;
        Draw.sprite(TextureId.POINTER, Input.mouseX() - cursorSize / 2, Input.mouseY() - cursorSize / 2,
                cursorSize, cursorSize);
    }
}
