package graphics;

import com.badlogic.gdx.Input.Keys;

import input.Input;
import player.Player;

/**
 * El inventario abierto (Tab o I): muestra la mochila encima de la barra rápida y deja ordenarlo arrastrando con el
 * ratón. El juego sigue en marcha mientras tanto, pero el ratón es para el inventario: no se dispara ni se construye.
 *
 * <p>Al soltar lo arrastrado:
 * <ul>
 *   <li>sobre una casilla: se mueve allí, juntándose con una pila del mismo objeto o intercambiándose con lo que
 *       haya ({@link items.Inventory#move});</li>
 *   <li>fuera del panel: se tira al suelo la pila entera;</li>
 *   <li>dentro del panel pero entre casillas: no pasa nada.</li>
 * </ul>
 */
public final class InventoryPanel {
    private static boolean open;
    /** Casilla de la que se está arrastrando, o -1. */
    private static int dragFrom = -1;

    private InventoryPanel() {
    }

    public static boolean isOpen() {
        return open;
    }

    /** Abre o cierra el inventario. Al cerrarlo se cancela lo que se estuviera arrastrando. */
    public static void toggle() {
        open = !open;
        dragFrom = -1;
    }

    /** Casilla que se está arrastrando, o -1. */
    static int draggedSlot() {
        return dragFrom;
    }

    /** Una vez por frame, aunque el juego esté en pausa. */
    public static void update() {
        if (Input.wasKeyPressed(Keys.TAB) || Input.wasKeyPressed(Keys.I)) {
            toggle();
        }
        if (!open) {
            return;
        }
        if (dragFrom < 0) {
            if (Input.wasButtonPressed(0)) {
                startDrag(Input.mouseX(), Input.mouseY());
            }
        } else if (!Input.isButtonDown(0)) {
            release(Input.mouseX(), Input.mouseY());
        }
    }

    /** Empieza a arrastrar lo que haya en la casilla bajo (x, y), si hay algo. */
    static void startDrag(float x, float y) {
        int slot = InventoryLayout.slotAt(x, y, true);
        if (slot >= 0 && Player.inventory.get(slot) != null) {
            dragFrom = slot;
        }
    }

    /** Suelta lo arrastrado en (x, y): ver la descripción de la clase. */
    static void release(float x, float y) {
        int target = InventoryLayout.slotAt(x, y, true);
        if (target >= 0) {
            Player.inventory.move(dragFrom, target);
        } else if (!InventoryLayout.panelContains(x, y)) {
            Player.dropStack(dragFrom);
        }
        dragFrom = -1;
    }
}
