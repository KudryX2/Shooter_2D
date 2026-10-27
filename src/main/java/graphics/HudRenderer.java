package graphics;

import static graphics.Sprite.drawSprite;
import static graphics.TexturesManager.tex;

import org.lwjgl.input.Mouse;

import assets.TextureId;
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

        // Objetos del inventario
        for (int i = 0; i < Player.inventory.size(); i++) {
            float x = width / 2 + 5 + (CELL_SPACING + hudScale) * i;
            drawSprite(tex(Player.inventory.get(i).icon), x, cellY + 5, 40 + hudScale, 40 + hudScale);
        }

        // Casillas de la mano
        for (int i = 0; i < Player.HAND_SLOTS; i++) {
            float x = width / 2 + (CELL_SPACING + hudScale) * i;
            drawSprite(tex(TextureId.NORMAL_CELL), x, cellY, cellSize, cellSize);
            if (i == Player.inventory.selectedSlot()) {
                drawSprite(tex(TextureId.SELECTED_CELL), x, cellY, cellSize, cellSize);
            }
        }

        PauseButton.draw();

        // Cursor (siempre lo último, por encima de todo el HUD). Con un objeto construible, la vista previa en el
        // mundo ya indica si se puede construir.
        drawSprite(tex(TextureId.POINTER), Mouse.getX() - 20 - hudScale / 2,
                height - hudScale / 2 - Mouse.getY() - 20, 40 + hudScale, 40 + hudScale);
    }
}
