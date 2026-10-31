package graphics;

import player.Player;
import world.Container;

/**
 * Dónde va cada casilla del inventario en pantalla (con el origen arriba a la izquierda), y cuál hay bajo un punto.
 *
 * <p>La barra rápida va centrada abajo. Con el inventario abierto, la mochila va encima, separada por un hueco tan
 * alto como la barra, en filas del mismo ancho que ella: la casilla {@code HOTBAR_SLOTS} es la primera de la fila de
 * arriba. Con un contenedor abierto, sus casillas van encima de la mochila, separadas igual, y se numeran a
 * continuación de las del jugador (a partir de {@link #CONTAINER_FIRST}).
 *
 * <p>Las casillas (16x16 px de pixel art) se amplían un número entero de veces según {@link HudRenderer#hudScale},
 * para que todos los píxeles salgan iguales.
 */
final class InventoryLayout {
    /** Tamaño de casilla de partida (más {@link HudRenderer#hudScale}) en píxeles de pantalla. */
    private static final int BASE_CELL_SIZE = 50;
    /** Lado de la imagen de una casilla, en píxeles del dibujo. */
    private static final int CELL_PIXELS = 16;
    private static final int COLUMNS = Player.HOTBAR_SLOTS;
    static final int BACKPACK_ROWS = Player.BACKPACK_SLOTS / COLUMNS;
    /** Número de la primera casilla del contenedor abierto (las de antes son las del jugador). */
    static final int CONTAINER_FIRST = Player.HOTBAR_SLOTS + Player.BACKPACK_SLOTS;
    private static final int CONTAINER_ROWS = Container.SLOTS / COLUMNS;
    /** Margen del fondo del panel alrededor de las casillas, en píxeles del dibujo. */
    static final int PANEL_PADDING = 3;

    private InventoryLayout() {
    }

    /** Píxeles de pantalla por píxel del dibujo de las casillas. */
    static int pixel() {
        return Math.max(1, Math.round((BASE_CELL_SIZE + HudRenderer.hudScale) / CELL_PIXELS));
    }

    /** Los iconos, un píxel de pantalla menos que las casillas: así caben dentro con margen. */
    static int iconPixel() {
        return Math.max(1, pixel() - 1);
    }

    static float cellSize() {
        return CELL_PIXELS * pixel();
    }

    /** De una casilla a la siguiente: un píxel de separación. */
    private static float step() {
        return cellSize() + pixel();
    }

    private static float hotbarY() {
        return Math.round(DisplayManager.HEIGHT / 1.1f);
    }

    private static float firstX() {
        return Math.round((DisplayManager.WIDTH - step() * COLUMNS) / 2);
    }

    /** Esquina superior izquierda de la casilla {@code slot}. */
    static float slotX(int slot) {
        return firstX() + step() * (slot % COLUMNS);
    }

    static float slotY(int slot) {
        if (slot < Player.HOTBAR_SLOTS) {
            return hotbarY();
        }
        if (slot >= CONTAINER_FIRST) {
            int row = (slot - CONTAINER_FIRST) / COLUMNS;
            return containerBottom() - cellSize() - (CONTAINER_ROWS - 1 - row) * step();
        }
        int row = (slot - Player.HOTBAR_SLOTS) / COLUMNS; // 0 = la de arriba
        float bottomRowTop = backpackBottom() - cellSize();
        return bottomRowTop - (BACKPACK_ROWS - 1 - row) * step();
    }

    /** Borde de abajo de las casillas del contenedor: separado de la mochila igual que ella de la barra. */
    private static float containerBottom() {
        return backpackPanelTop() - hotbarPanelHeight() - PANEL_PADDING * pixel();
    }

    /**
     * Borde de abajo de las casillas de la mochila: entre su fondo y el de la barra rápida queda un hueco tan alto
     * como el fondo de la barra.
     */
    private static float backpackBottom() {
        return hotbarPanelTop() - hotbarPanelHeight() - PANEL_PADDING * pixel();
    }

    /**
     * Casilla bajo (x, y), o -1 si no hay ninguna. Las de la mochila solo cuentan si {@code withBackpack} (si el
     * inventario está abierto) y las del contenedor, si {@code withContainer} (si hay uno abierto).
     */
    static int slotAt(float x, float y, boolean withBackpack, boolean withContainer) {
        int slots = withContainer ? CONTAINER_FIRST + Container.SLOTS
                : withBackpack ? CONTAINER_FIRST : Player.HOTBAR_SLOTS;
        float size = cellSize();
        for (int slot = 0; slot < slots; slot++) {
            float left = slotX(slot), top = slotY(slot);
            if (x >= left && x < left + size && y >= top && y < top + size) {
                return slot;
            }
        }
        return -1;
    }

    // Fondos del inventario abierto: uno detrás de la mochila y otro detrás de la barra rápida, con el mismo ancho

    static float panelLeft() {
        return firstX() - PANEL_PADDING * pixel();
    }

    static float panelWidth() {
        return step() * COLUMNS - pixel() + 2 * PANEL_PADDING * pixel();
    }

    static float containerPanelTop() {
        return slotY(CONTAINER_FIRST) - PANEL_PADDING * pixel();
    }

    static float containerPanelHeight() {
        return containerBottom() + PANEL_PADDING * pixel() - containerPanelTop();
    }

    static float backpackPanelTop() {
        return slotY(Player.HOTBAR_SLOTS) - PANEL_PADDING * pixel();
    }

    static float backpackPanelHeight() {
        return backpackBottom() + PANEL_PADDING * pixel() - backpackPanelTop();
    }

    static float hotbarPanelTop() {
        return hotbarY() - PANEL_PADDING * pixel();
    }

    static float hotbarPanelHeight() {
        return cellSize() + 2 * PANEL_PADDING * pixel();
    }

    /**
     * (x, y) cae dentro del inventario abierto (con el contenedor, si {@code withContainer}): en los fondos o en los
     * huecos entre ellos (soltar algo ahí no lo tira al suelo, por si se escapa el ratón).
     */
    static boolean panelContains(float x, float y, boolean withContainer) {
        float top = withContainer ? containerPanelTop() : backpackPanelTop();
        float bottom = hotbarPanelTop() + hotbarPanelHeight();
        return x >= panelLeft() && x < panelLeft() + panelWidth() && y >= top && y < bottom;
    }
}
