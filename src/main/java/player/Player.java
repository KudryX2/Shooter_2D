package player;

import static util.Geometry.distance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import graphics.DisplayManager;
import input.Input;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.Item;
import items.StructItem;
import structs.Struct;
import structs.StructKind;
import util.GameClock;
import world.Chunk;
import world.WorldManager;

public final class Player {
    public static final float SIZE = 35;
    public static final float SPAWN_X = 600;
    public static final float SPAWN_Y = 600;
    public static final int HAND_SLOTS = 4;
    public static final int MAX_HP = 100;
    private static final float PICKUP_RANGE = 50;
    private static final float BUILD_RANGE = 250;
    private static final float WALL_MARGIN = 10;
    private static final float HAND_OFFSET = 40;

    public static float x, y;
    /** Estructura que se construiría ahora con el objeto seleccionado (vista previa), o {@code null}. */
    public static Struct buildPreview;
    public static float rotation;
    public static float speed = 0.5f;
    public static int hp;

    public static int selectedCell;
    public static final List<Item> inventory = new ArrayList<>();

    public static boolean canBuild;

    /**
     * Punto del mundo al que apunta el cursor, actualizado en cada frame. Lo usa el mundo para los headshots,
     * así la lógica del juego no lee el ratón. NaN = no apunta a nada.
     */
    public static float aimX = Float.NaN, aimY = Float.NaN;

    /** Más allá de esta distancia (en cada eje) un muro no puede bloquear al jugador: medio jugador + margen. */
    private static final float COLLISION_RANGE = SIZE / 2 + WALL_MARGIN;

    static boolean collUp, collDown, collLeft, collRight; // visibles para los tests del paquete

    private static boolean clickStartedOnUi;

    private Player() {
    }

    public static void create() {
        x = SPAWN_X;
        y = SPAWN_Y;
        hp = MAX_HP;
        selectedCell = 0;
        buildPreview = null;

        Mouse.setCursorPosition(DisplayManager.WIDTH / 2, DisplayManager.HEIGHT / 2);
    }

    /**
     * @param mouseOnUi el cursor está sobre un botón del HUD: un clic que empieza ahí no dispara (hasta soltarlo)
     */
    public static void update(boolean mouseOnUi) {
        float step = speed * GameClock.delta();
        if (Input.isKeyDown(Keyboard.KEY_W) && !collUp) y -= step;
        if (Input.isKeyDown(Keyboard.KEY_S) && !collDown) y += step;
        if (Input.isKeyDown(Keyboard.KEY_A) && !collLeft) x -= step;
        if (Input.isKeyDown(Keyboard.KEY_D) && !collRight) x += step;
        aimX = cursorWorldX();
        aimY = cursorWorldY();

        rotation = -(float) Math.toDegrees(Math.atan2(
                Mouse.getY() - DisplayManager.HEIGHT / 2f, Mouse.getX() - DisplayManager.WIDTH / 2f));

        updateSelectedCell();
        updateBuildPosition(); // antes de los clics, con el objeto que hay ahora en la mano

        // Acciones
        if (Input.wasButtonPressed(0)) clickStartedOnUi = mouseOnUi;
        if (Input.isButtonDown(0) && !clickStartedOnUi) primaryAction(Input.wasButtonPressed(0));
        if (Input.wasButtonPressed(1)) secondaryAction();
        if (Input.wasKeyPressed(Keyboard.KEY_Q)) dropSelectedItem();
        if (Input.wasKeyPressed(Keyboard.KEY_E)) pickUpItem();

        checkCollisions();

        if (hp <= 0) {
            die();
            respawn();
        }
    }

    private static void updateSelectedCell() {
        if (Input.isKeyDown(Keyboard.KEY_1)) selectedCell = 0;
        if (Input.isKeyDown(Keyboard.KEY_2)) selectedCell = 1;
        if (Input.isKeyDown(Keyboard.KEY_3)) selectedCell = 2;
        if (Input.isKeyDown(Keyboard.KEY_4)) selectedCell = 3;

        int wheel = Mouse.getDWheel();
        if (wheel < 0) {
            selectedCell = (selectedCell + 1) % HAND_SLOTS;
        } else if (wheel > 0) {
            selectedCell = (selectedCell + HAND_SLOTS - 1) % HAND_SLOTS;
        }
    }

    /** Objeto en la casilla seleccionada, o {@code null} si está vacía. */
    public static Item selectedItem() {
        return selectedCell < inventory.size() ? inventory.get(selectedCell) : null;
    }

    public static float cursorWorldX() {
        return x + Mouse.getX() - DisplayManager.WIDTH / 2f;
    }

    public static float cursorWorldY() {
        return y + DisplayManager.HEIGHT / 2f - Mouse.getY();
    }

    /** Decide en qué direcciones no puede moverse el jugador porque hay un muro o medio muro pegado. */
    static void checkCollisions() {
        collUp = collDown = collLeft = collRight = false;
        WorldManager.forEachSolidNear(x, y, COLLISION_RANGE, Player::checkCollision);
    }

    private static void checkCollision(Struct w) {
        float half = SIZE / 2;
        float sw = w.halfWidth(), sh = w.halfHeight();
        boolean overlapX = w.x() + sw > x - half && w.x() - sw < x + half;
        boolean overlapY = w.y() + sh > y - half && w.y() - sh < y + half;
        if (overlapX && w.y() + sh + WALL_MARGIN > y - half && w.y() - sh + WALL_MARGIN < y - half) collUp = true;
        if (overlapX && w.y() + sh - WALL_MARGIN > y + half && w.y() - sh - WALL_MARGIN < y + half) collDown = true;
        if (overlapY && w.x() + sw + WALL_MARGIN > x - half && w.x() - sw + WALL_MARGIN < x - half) collLeft = true;
        if (overlapY && w.x() + sw - WALL_MARGIN > x + half && w.x() - sw - WALL_MARGIN < x + half) collRight = true;
    }

    /** Botón izquierdo: dispara el arma o, con un objeto construible en la mano, construye (una vez por clic). */
    private static void primaryAction(boolean justPressed) {
        Item item = selectedItem();
        if (item instanceof Gun gun) {
            double angle = Math.toRadians(-rotation + 90);
            float shotX = x + (float) Math.sin(angle) * HAND_OFFSET;
            float shotY = y + (float) Math.cos(angle) * HAND_OFFSET;
            gun.shoot(x, y, shotX, shotY, rotation, true, justPressed);
        } else if (justPressed && canBuild && buildPreview != null) {
            WorldManager.build(buildPreview);
            canBuild = false; // ese borde ya está ocupado hasta el siguiente frame
        }
    }

    /**
     * Botón derecho: con un objeto construible en la mano derriba lo apuntado; con un consumible, lo usa. Con
     * un arma no hace nada, para no derribar muros sin querer.
     */
    private static void secondaryAction() {
        Item item = selectedItem();
        if (item instanceof StructItem) {
            demolish(cursorWorldX(), cursorWorldY());
        } else {
            useSelectedConsumable();
        }
    }

    /**
     * Derriba, si está al alcance, el muro del borde más cercano a (wx, wy) (el mismo que marcaría la vista
     * previa); si no hay, un suelo construido en esa casilla.
     *
     * @return {@code true} si ha derribado algo
     */
    static boolean demolish(float wx, float wy) {
        Struct edge = Struct.snapped(wx, wy, StructKind.WALL, null);
        if (distance(edge.x(), edge.y(), x, y) <= BUILD_RANGE && WorldManager.removeWallAt(edge)) {
            return true;
        }
        Struct tile = Struct.snapped(wx, wy, StructKind.FLOOR, null);
        return distance(tile.x(), tile.y(), x, y) <= BUILD_RANGE
                && WorldManager.removeBuiltFloorAt(tile.x(), tile.y());
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
        inventory.remove(selectedCell);
        return true;
    }

    private static void updateBuildPosition() {
        if (!(selectedItem() instanceof StructItem item)) {
            buildPreview = null;
            canBuild = false;
            return;
        }
        buildPreview = Struct.snapped(cursorWorldX(), cursorWorldY(), item.kind, item.icon);
        canBuild = canBuild(buildPreview);
    }

    /**
     * Se puede construir al alcance, y un muro solo en un borde libre y sin quedar encima del jugador. Si deja
     * huecos con los muros vecinos es cosa del jugador.
     */
    static boolean canBuild(Struct struct) {
        if (distance(struct.x(), struct.y(), x, y) > BUILD_RANGE) {
            return false;
        }
        if (!struct.isWall()) {
            return true;
        }
        return !WorldManager.hasWallAt(struct) && !struct.overlaps(x, y, SIZE / 2, SIZE / 2);
    }

    public static void dropSelectedItem() {
        Item item = selectedItem();
        if (item != null) {
            WorldManager.dropItem(item, x, y);
            inventory.remove(selectedCell);
        }
    }

    public static void pickUpItem() {
        if (inventory.size() >= HAND_SLOTS) {
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

    public static void die() {
        for (Item item : inventory) {
            WorldManager.dropItem(item, x, y);
        }
        inventory.clear();
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
