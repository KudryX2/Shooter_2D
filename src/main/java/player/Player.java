package player;

import static util.Geometry.angleTo;
import static util.Geometry.dirX;
import static util.Geometry.dirY;
import static util.Geometry.distance;

import java.util.ListIterator;

import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.Inventory;
import items.Item;
import items.ItemStack;
import items.MeleeWeapon;
import items.Projectile;
import items.StructItem;
import structs.Struct;
import structs.StructKind;
import util.GameClock;
import util.WalkCycle;
import world.Chunk;
import world.Movement;
import world.WorldManager;

/**
 * El jugador: posición, vida, inventario y lo que puede hacer. No lee el teclado ni el ratón: eso lo hace
 * {@link PlayerController}, que llama a estos métodos.
 */
public final class Player {
    public static final float SIZE = 35;
    /** Punto de inicio si el mundo no pone otro ({@link #setSpawn}): el centro de la casilla (9, 9). */
    public static final float DEFAULT_SPAWN_X = 9 * Struct.SIZE;
    public static final float DEFAULT_SPAWN_Y = 9 * Struct.SIZE;
    /** Casillas de la barra rápida; se seleccionan con 1-9, 0 y la rueda. */
    public static final int HOTBAR_SLOTS = 10;
    /** Casillas de la mochila (3 filas de la barra rápida). */
    public static final int BACKPACK_SLOTS = 30;
    public static final int MAX_HP = 100;
    /** Distancia del centro a la boca del arma, de donde salen las balas (el arma se dibuja para que encaje). */
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
    public static final Inventory inventory = new Inventory(HOTBAR_SLOTS + BACKPACK_SLOTS, HOTBAR_SLOTS);
    /** Para la animación de andar. */
    public static final WalkCycle walkCycle = new WalkCycle();
    private static float spawnX = DEFAULT_SPAWN_X, spawnY = DEFAULT_SPAWN_Y;

    /**
     * Punto del mundo al que apunta, actualizado en cada frame. Lo usa el mundo para los headshots.
     * NaN = no apunta a nada.
     */
    public static float aimX = Float.NaN, aimY = Float.NaN;
    /** Arma que está recargando, o {@code null}. */
    private static Gun reloading;

    private Player() {
    }

    public static void create() {
        cancelReload();
        teleportToSpawn();
        hp = MAX_HP;
        inventory.select(0);
        BuildTool.update(null, x, y);
    }

    /** Avanza la recarga y muere si se ha quedado sin vida. Una vez por frame, después de sus acciones. */
    public static void update() {
        updateReload();
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
        float startX = x, startY = y;
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
        walkCycle.step(distance(startX, startY, x, y));
    }

    /**
     * Apunta (y mira) hacia el punto del mundo (wx, wy), donde está el cursor. Las balas vuelan
     * {@link Projectile#FLIGHT_HEIGHT} por encima del suelo, así que se apunta a ese tanto más abajo: la bala que se
     * ve pasa justo por el cursor.
     */
    public static void aimAt(float wx, float wy) {
        aimX = wx;
        aimY = wy;
        float groundY = wy + Projectile.FLIGHT_HEIGHT;
        if (wx != x || groundY != y) {
            rotation = angleTo(x, y, wx, groundY);
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

    /**
     * Botón izquierdo: dispara el arma, golpea con el arma cuerpo a cuerpo (manteniéndolo, una y otra vez) o, con un
     * objeto construible en la mano, construye (una vez por clic).
     */
    public static void primaryAction(boolean justPressed) {
        Item item = selectedItem();
        if (item instanceof Gun gun) {
            float muzzleX = x + dirX(rotation) * HAND_OFFSET;
            float muzzleY = y + dirY(rotation) * HAND_OFFSET;
            gun.shoot(x, y, muzzleX, muzzleY, rotation, true, justPressed);
        } else if (item instanceof MeleeWeapon melee) {
            melee.swing(x, y, rotation);
        } else if (item instanceof StructItem && justPressed && BuildTool.build()) {
            inventory.removeSelected(); // gasta el material
        }
    }

    /**
     * Botón derecho: con un objeto construible en la mano derriba lo apuntado (y recupera su material: al
     * inventario o, si no cabe, al suelo); con un consumible, lo usa. Con un arma no hace nada, para no derribar
     * muros sin querer.
     */
    public static void secondaryAction() {
        if (selectedItem() instanceof StructItem) {
            StructKind demolished = BuildTool.demolish(aimX, aimY);
            if (demolished != null) {
                StructItem material = new StructItem(demolished);
                if (!inventory.add(material)) {
                    WorldManager.dropItem(material, x, y);
                }
            }
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

    // ---------------------------------------------------------------------------------------------
    // Recarga
    // ---------------------------------------------------------------------------------------------

    /** R: recarga el arma de la mano, si le faltan balas y quedan en alguna caja de su munición. */
    public static void reload() {
        if (reloading == null && selectedItem() instanceof Gun gun && gun.startReload(inventory)) {
            reloading = gun;
        }
    }

    /**
     * Termina la recarga en curso cuando le toca. Si el arma ya no está en la mano (se ha cambiado de casilla o se ha
     * soltado), se deja a medias sin gastar balas.
     */
    private static void updateReload() {
        if (reloading == null) {
            return;
        }
        if (selectedItem() != reloading) {
            cancelReload();
        } else if (reloading.updateReload(inventory)) {
            reloading = null;
        }
    }

    private static void cancelReload() {
        if (reloading != null) {
            reloading.cancelReload();
            reloading = null;
        }
    }

    /** Cuánto lleva la recarga en curso, de 0 a 1, o -1 si no está recargando (para su barra de progreso). */
    public static float reloadProgress() {
        return reloading == null ? -1 : reloading.reloadProgress();
    }

    /** Tira al suelo la pila entera de la casilla {@code slot} (p.ej. arrastrándola fuera del inventario). */
    public static void dropStack(int slot) {
        ItemStack stack = inventory.removeStack(slot);
        if (stack != null) {
            WorldManager.dropItem(stack, x, y);
        }
    }

    /**
     * Coge un objeto (o un montón) del suelo que esté al lado y quepa en el inventario. Si del montón no cabe todo,
     * lo que sobra se queda en el suelo.
     */
    public static void pickUpItem() {
        for (Chunk chunk : WorldManager.activeChunks) {
            ListIterator<DroppedItem> it = chunk.droppedItems.listIterator();
            while (it.hasNext()) {
                DroppedItem dropped = it.next();
                if (distance(dropped.x(), dropped.y(), x, y) < PICKUP_RANGE && inventory.canAdd(dropped.item())) {
                    int left = inventory.add(dropped.item(), dropped.count());
                    if (left == 0) {
                        it.remove();
                    } else {
                        it.set(new DroppedItem(dropped.item(), left, dropped.x(), dropped.y()));
                    }
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
        cancelReload();
        for (ItemStack stack : inventory.removeAll()) {
            WorldManager.dropItem(stack, x, y);
        }
    }

    public static void respawn() {
        teleportToSpawn();
        hp = MAX_HP;
    }

    public static void teleportToSpawn() {
        x = spawnX;
        y = spawnY;
    }

    /** Cada mundo pone su punto de inicio al crearse (donde aparece, reaparece y vuelve con la R). */
    public static void setSpawn(float x, float y) {
        spawnX = x;
        spawnY = y;
    }

    public static float spawnX() {
        return spawnX;
    }

    public static float spawnY() {
        return spawnY;
    }
}
