package graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import input.Input;
import items.Item;
import player.Player;

/**
 * Inventario (la barra rápida y, abierto, la mochila: ver {@link InventoryLayout} e {@link InventoryPanel}), botón de
 * pausa y cursor, en coordenadas de pantalla.
 */
public final class HudRenderer {
    private static final float CURSOR_SIZE = 26;
    /** Distancia del número de una pila al borde de su casilla, en píxeles del dibujo (el marco de selección mide 2). */
    private static final int COUNT_INSET = 2;
    /** Opacidad del fondo del inventario abierto (deja ver un poco el mundo, que sigue en marcha). */
    private static final float PANEL_ALPHA = 0.55f;
    /** Opacidad de lo que queda en una casilla mientras se arrastra (para ver de dónde sale). */
    private static final float DRAGGED_SOURCE_ALPHA = 0.3f;

    /** Escala del HUD (teclas N/M). */
    public static float hudScale;

    private HudRenderer() {
    }

    static void init() {
        hudScale = DisplayManager.WIDTH / 100f;
    }

    static void draw() {
        boolean open = InventoryPanel.isOpen();
        if (open) {
            drawPanelBackground();
        }
        int slots = open ? Player.inventory.capacity() : Player.HOTBAR_SLOTS;
        for (int slot = 0; slot < slots; slot++) {
            drawSlot(slot);
        }
        drawDragged();

        PauseButton.draw();

        // Cursor (siempre lo último, por encima de todo el HUD). Con un objeto construible, la vista previa en el
        // mundo ya indica si se puede construir.
        // Pixel art de 13x13 a 2 px por píxel: más o menos del tamaño de una cabeza en pantalla (~22 px), para poder
        // apuntar a ella. Tamaño fijo (no crece con el HUD) para que los píxeles salgan parejos.
        float cursorSize = CURSOR_SIZE;
        Draw.sprite(TextureId.POINTER, Input.mouseX() - cursorSize / 2, Input.mouseY() - cursorSize / 2,
                cursorSize, cursorSize);
    }

    /** Fondos oscuros detrás de la mochila y de la barra rápida (por separado), con el inventario abierto. */
    private static void drawPanelBackground() {
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0.05f, 0.04f, 0.04f, PANEL_ALPHA);
        float left = InventoryLayout.panelLeft(), width = InventoryLayout.panelWidth();
        shapes.rect(left, InventoryLayout.backpackPanelTop(), width, InventoryLayout.backpackPanelHeight());
        shapes.rect(left, InventoryLayout.hotbarPanelTop(), width, InventoryLayout.hotbarPanelHeight());
    }

    /** Una casilla con su contenido (atenuado si se está arrastrando), el marco si es la elegida y la cantidad. */
    private static void drawSlot(int slot) {
        float x = InventoryLayout.slotX(slot), y = InventoryLayout.slotY(slot);
        float size = InventoryLayout.cellSize();
        Draw.sprite(TextureId.NORMAL_CELL, x, y, size, size);
        Item item = Player.inventory.get(slot);
        int count = Player.inventory.count(slot);
        boolean dragged = slot == InventoryPanel.draggedSlot();
        if (item != null) {
            if (dragged) {
                Draw.tint(1, 1, 1, DRAGGED_SOURCE_ALPHA);
            }
            drawStack(item, count, x + size / 2, y + size / 2);
            Draw.tint(1, 1, 1, 1);
        }
        if (slot == Player.inventory.selectedSlot()) {
            Draw.sprite(TextureId.SELECTED_CELL, x, y, size, size);
        }
        if (count > 1 && !dragged) {
            // Abajo a la derecha, por dentro del marco de selección y encima de él, para que no lo tape
            drawCount(count, x + size, y + size);
        }
    }

    /** Lo que se está arrastrando, centrado en el cursor y con su cantidad. */
    private static void drawDragged() {
        int slot = InventoryPanel.draggedSlot();
        if (slot < 0 || Player.inventory.get(slot) == null) {
            return;
        }
        int count = Player.inventory.count(slot);
        float centerX = Math.round(Input.mouseX()), centerY = Math.round(Input.mouseY());
        drawStack(Player.inventory.get(slot), count, centerX, centerY);
        if (count > 1) {
            float half = InventoryLayout.cellSize() / 2;
            drawCount(count, centerX + half, centerY + half);
        }
    }

    /** Icono (amontonado si es una pila) centrado en (centerX, centerY), a los píxeles enteros de los iconos. */
    private static void drawStack(Item item, int count, float centerX, float centerY) {
        int iconPixel = InventoryLayout.iconPixel();
        TextureRegion icon = TexturesManager.region(item.icon);
        float width = icon.getRegionWidth() * iconPixel, height = icon.getRegionHeight() * iconPixel;
        StackDrawing.draw(icon, Math.round(centerX - width / 2), Math.round(centerY - height / 2), width, height, count,
                iconPixel);
    }

    /** Cantidad acabando abajo a la derecha de una casilla cuya esquina inferior derecha es (right, bottom). */
    private static void drawCount(int count, float right, float bottom) {
        int pixel = InventoryLayout.pixel();
        float inset = COUNT_INSET * pixel;
        StackDrawing.number(count, right - inset, bottom - inset, Math.max(2, pixel / 2));
    }
}
