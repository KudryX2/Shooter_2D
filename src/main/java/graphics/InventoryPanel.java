package graphics;

import com.badlogic.gdx.Input.Keys;

import input.Input;
import items.Inventory;
import player.Player;
import world.Container;

/**
 * El inventario abierto (Tab o I): muestra la mochila encima de la barra rápida y deja ordenarlo arrastrando con el
 * ratón. Con un contenedor abierto (E junto a una caja) se abre solo y muestra también sus casillas, encima; si se
 * cierra el contenedor (al alejarse o romperse), se cierra con él. El juego sigue en marcha mientras tanto, pero el
 * ratón es para el inventario: no se dispara ni se construye.
 *
 * <p>Las casillas se numeran como en {@link InventoryLayout}: primero las del jugador y luego las del contenedor. Al
 * soltar lo arrastrado:
 * <ul>
 *   <li>sobre una casilla (del jugador o del contenedor): se mueve allí, juntándose con una pila del mismo objeto o
 *       intercambiándose con lo que haya ({@link Inventory#move(Inventory, int, Inventory, int)});</li>
 *   <li>fuera de los paneles: se tira al suelo la pila entera, junto al jugador;</li>
 *   <li>dentro de los paneles pero entre casillas: no pasa nada.</li>
 * </ul>
 *
 * <p>Con un contenedor abierto, Mayús + clic derecho sobre una casilla pasa su pila entera al otro lado
 * ({@link #quickMove}).
 */
public final class InventoryPanel {
    private static boolean open;
    /** Se ha abierto al abrir un contenedor: se cierra con él. */
    private static boolean openedWithContainer;
    /** Casilla de la que se está arrastrando, o -1. */
    private static int dragFrom = -1;

    private InventoryPanel() {
    }

    public static boolean isOpen() {
        return open;
    }

    /** Abre o cierra el inventario. Al cerrarlo se cierra también el contenedor y se cancela lo que se arrastraba. */
    public static void toggle() {
        open = !open;
        openedWithContainer = false;
        dragFrom = -1;
        if (!open) {
            Player.closeContainer();
        }
    }

    /** Casilla que se está arrastrando, o -1. */
    static int draggedSlot() {
        return dragFrom;
    }

    /** El contenedor abierto, o {@code null}. */
    static Container container() {
        return Player.openContainer();
    }

    /** Inventario al que pertenece la casilla {@code slot} (del jugador o del contenedor abierto). */
    static Inventory inventoryOf(int slot) {
        return slot < InventoryLayout.CONTAINER_FIRST ? Player.inventory : container().inventory;
    }

    /** La casilla {@code slot} dentro de su inventario. */
    static int localSlot(int slot) {
        return slot < InventoryLayout.CONTAINER_FIRST ? slot : slot - InventoryLayout.CONTAINER_FIRST;
    }

    /** Una vez por frame, aunque el juego esté en pausa. */
    public static void update() {
        if (Input.wasKeyPressed(Keys.TAB) || Input.wasKeyPressed(Keys.I)) {
            toggle();
        }
        followContainer();
        if (!open) {
            return;
        }
        if (dragFrom < 0) {
            if (Input.wasButtonPressed(0)) {
                startDrag(Input.mouseX(), Input.mouseY());
            } else if (Input.wasButtonPressed(1)
                    && (Input.isKeyDown(Keys.SHIFT_LEFT) || Input.isKeyDown(Keys.SHIFT_RIGHT))) {
                quickMove(Input.mouseX(), Input.mouseY());
            }
        } else if (!Input.isButtonDown(0)) {
            release(Input.mouseX(), Input.mouseY());
        }
    }

    /** Se abre al abrir un contenedor y, si se abrió así, se cierra con él. */
    static void followContainer() {
        boolean hasContainer = container() != null;
        if (hasContainer && !open) {
            open = true;
            openedWithContainer = true;
        } else if (!hasContainer && open && openedWithContainer) {
            open = false;
            openedWithContainer = false;
        }
        if (!hasContainer && dragFrom >= InventoryLayout.CONTAINER_FIRST) {
            dragFrom = -1; // lo que se arrastraba era del contenedor, que ya no está
        }
        if (!open) {
            dragFrom = -1;
        }
    }

    /** Empieza a arrastrar lo que haya en la casilla bajo (x, y), si hay algo. */
    static void startDrag(float x, float y) {
        int slot = InventoryLayout.slotAt(x, y, true, container() != null);
        if (slot >= 0 && inventoryOf(slot).get(localSlot(slot)) != null) {
            dragFrom = slot;
        }
    }

    /**
     * Mayús + clic derecho sobre la casilla bajo (x, y), con un contenedor abierto: pasa su pila entera al otro lado
     * (del contenedor al jugador o del jugador al contenedor). Lo que no quepa se queda donde estaba.
     */
    static void quickMove(float x, float y) {
        Container container = container();
        if (container == null) {
            return;
        }
        int slot = InventoryLayout.slotAt(x, y, true, true);
        if (slot < 0) {
            return;
        }
        Inventory target = slot < InventoryLayout.CONTAINER_FIRST ? container.inventory : Player.inventory;
        inventoryOf(slot).transfer(localSlot(slot), target);
    }

    /** Suelta lo arrastrado en (x, y): ver la descripción de la clase. */
    static void release(float x, float y) {
        boolean withContainer = container() != null;
        int target = InventoryLayout.slotAt(x, y, true, withContainer);
        if (target >= 0) {
            Inventory.move(inventoryOf(dragFrom), localSlot(dragFrom), inventoryOf(target), localSlot(target));
        } else if (!InventoryLayout.panelContains(x, y, withContainer)) {
            Player.dropStack(inventoryOf(dragFrom), localSlot(dragFrom));
        }
        dragFrom = -1;
    }
}
