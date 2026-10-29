package player;

import static util.Geometry.angleTo;
import static util.Geometry.dirX;
import static util.Geometry.dirY;
import static util.Geometry.distance;

import java.util.Iterator;

import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.Inventory;
import items.Item;
import items.StructItem;
import structs.Struct;
import util.GameClock;
import world.Chunk;
import world.Movement;
import world.WorldManager;

/**
 * El jugador: posición, vida, inventario y lo que puede hacer. No lee el teclado ni el ratón: eso lo hace
 * {@link PlayerController}, que llama a estos métodos.
 */
public final class Player {
    public static final float SIZE = 35;
    /** En el centro de una casilla (la 9, 9), que es donde se construye la casa inicial del Puesto avanzado. */
    public static final float SPAWN_X = 9 * Struct.SIZE;
    public static final float SPAWN_Y = 9 * Struct.SIZE;
    /** Casillas del inventario; se seleccionan con 1-9, 0 y la rueda. */
    public static final int INVENTORY_SLOTS = 10;
    public static final int MAX_HP = 100;
    /** Distancia del centro a la mano (donde se dibuja el arma y salen las balas). */
    public static final float HAND_OFFSET = 40;
    /** Velocidad en px/ms. */
    private static final float SPEED = 0.5f;
    /** Paso máximo entre comprobaciones de muros, para no atravesar uno fino si un frame va lento. */
    private static final float MAX_STEP = 8;
    private static final float PICKUP_RANGE = 50;

    public static float x, y;
    /** Hacia dónde mira, en grados (ver {@link util.Geometry}). */
    public static float rotation;
    public static int hp;
    public static final Inventory inventory = new Inventory(INVENTORY_SLOTS);

    /**
     * Punto del mundo al que apunta, actualizado en cada frame. Lo usa el mundo para los headshots.
     * NaN = no apunta a nada.
     */
    public static float aimX = Float.NaN, aimY = Float.NaN;

    private Player() {
    }

    public static void create() {
        teleportToSpawn();
        hp = MAX_HP;
        inventory.select(0);
        BuildTool.update(null, x, y);
    }

    /** Muere si se ha quedado sin vida. Una vez por frame, después de sus acciones. */
    public static void update() {
        if (hp <= 0) {
            die();
            respawn();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Movimiento y puntería
    // ---------------------------------------------------------------------------------------------

    /**
     * Anda en la dirección (dx, dy), cada una -1, 0 o 1. Cada eje se mueve por separado, así que al chocar con
     * un muro en diagonal se desliza a lo largo de él.
     */
    public static void move(int dx, int dy) {
        float distance = SPEED * GameClock.delta();
        int steps = Math.max(1, (int) Math.ceil(distance / MAX_STEP));
        float step = distance / steps;
        float half = SIZE / 2;
        for (int i = 0; i < steps; i++) {
            float nextX = x + dx * step;
            if (Movement.canMove(x, y, nextX, y, half)) {
                x = nextX;
            }
            float nextY = y + dy * step;
            if (Movement.canMove(x, y, x, nextY, half)) {
                y = nextY;
            }
        }
    }

    /** Apunta (y mira) hacia el punto del mundo (wx, wy). */
    public static void aimAt(float wx, float wy) {
        aimX = wx;
        aimY = wy;
        if (wx != x || wy != y) {
            rotation = angleTo(x, y, wx, wy);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Acciones
    // ---------------------------------------------------------------------------------------------

    public static Item selectedItem() {
        return inventory.selectedItem();
    }

    /** Recalcula la vista previa de construcción. Antes de las acciones, con el objeto que hay en la mano. */
    public static void updateBuildPreview() {
        BuildTool.update(selectedItem(), aimX, aimY);
    }

    /** Botón izquierdo: dispara el arma o, con un objeto construible en la mano, construye (una vez por clic). */
    public static void primaryAction(boolean justPressed) {
        Item item = selectedItem();
        if (item instanceof Gun gun) {
            float muzzleX = x + dirX(rotation) * HAND_OFFSET;
            float muzzleY = y + dirY(rotation) * HAND_OFFSET;
            gun.shoot(x, y, muzzleX, muzzleY, rotation, true, justPressed);
        } else if (item instanceof StructItem && justPressed) {
            BuildTool.build();
        }
    }

    /**
     * Botón derecho: con un objeto construible en la mano derriba lo apuntado; con un consumible, lo usa. Con
     * un arma no hace nada, para no derribar muros sin querer.
     */
    public static void secondaryAction() {
        if (selectedItem() instanceof StructItem) {
            BuildTool.demolish(aimX, aimY);
        } else {
            useSelectedConsumable();
        }
    }

    /**
     * Usa el consumible de la casilla seleccionada (p.ej. un botiquín) y lo gasta.
     * Con la vida al máximo no hace nada, para no malgastarlo.
     *
     * @return {@code true} si se ha usado
     */
    public static boolean useSelectedConsumable() {
        if (!(selectedItem() instanceof Consumable consumable) || hp >= MAX_HP) {
            return false;
        }
        hp = Math.min(MAX_HP, hp + consumable.heal);
        inventory.removeSelected();
        return true;
    }

    public static void dropSelectedItem() {
        Item item = inventory.removeSelected();
        if (item != null) {
            WorldManager.dropItem(item, x, y);
        }
    }

    /** Coge un objeto del suelo que esté al lado, si cabe en el inventario. */
    public static void pickUpItem() {
        if (inventory.isFull()) {
            return;
        }
        for (Chunk chunk : WorldManager.activeChunks) {
            Iterator<DroppedItem> it = chunk.droppedItems.iterator();
            while (it.hasNext()) {
                DroppedItem dropped = it.next();
                if (distance(dropped.x(), dropped.y(), x, y) < PICKUP_RANGE) {
                    it.remove();
                    inventory.add(dropped.item());
                    return;
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Vida
    // ---------------------------------------------------------------------------------------------

    /** Suelta todo lo que lleva donde está. */
    public static void die() {
        for (Item item : inventory.removeAll()) {
            WorldManager.dropItem(item, x, y);
        }
    }

    public static void respawn() {
        teleportToSpawn();
        hp = MAX_HP;
    }

    public static void teleportToSpawn() {
        x = SPAWN_X;
        y = SPAWN_Y;
    }
}
