package player;

import static util.Geometry.distance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import graphics.DisplayManager;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.Item;
import structs.Struct;
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
    public static float buildX, buildY;
    public static float rotation;
    public static float speed = 0.5f;
    public static int hp;
    public static int textureID;

    public static int selectedCell;
    public static final List<Item> inventory = new ArrayList<>();

    public static boolean buildMode, canBuild;

    /**
     * Punto del mundo al que apunta el cursor, actualizado en cada frame. Lo usa el mundo para los headshots,
     * así la lógica del juego no lee el ratón. NaN = no apunta a nada.
     */
    public static float aimX = Float.NaN, aimY = Float.NaN;

    /** Más allá de esta distancia (en cada eje) un muro no puede bloquear al jugador: medio muro + medio jugador + margen. */
    private static final float COLLISION_RANGE = Struct.SIZE / 2 + SIZE / 2 + WALL_MARGIN;

    static boolean collUp, collDown, collLeft, collRight; // visibles para los tests del paquete
    private static boolean wasButton0, wasButton1, wasQ, wasE, wasB;
    private static boolean clickStartedOnUi;

    private Player() {
    }

    public static void create() {
        x = SPAWN_X;
        y = SPAWN_Y;
        hp = MAX_HP;
        textureID = 0;
        selectedCell = 0;
        buildMode = false;

        Mouse.setCursorPosition(DisplayManager.WIDTH / 2, DisplayManager.HEIGHT / 2);
        buildX = cursorWorldX();
        buildY = cursorWorldY();
    }

    /**
     * @param mouseOnUi el cursor está sobre un botón del HUD: un clic que empieza ahí no dispara (hasta soltarlo)
     */
    public static void update(boolean mouseOnUi) {
        float step = speed * GameClock.delta();
        if (Keyboard.isKeyDown(Keyboard.KEY_W) && !collUp) y -= step;
        if (Keyboard.isKeyDown(Keyboard.KEY_S) && !collDown) y += step;
        if (Keyboard.isKeyDown(Keyboard.KEY_A) && !collLeft) x -= step;
        if (Keyboard.isKeyDown(Keyboard.KEY_D) && !collRight) x += step;
        aimX = cursorWorldX();
        aimY = cursorWorldY();

        rotation = -(float) Math.toDegrees(Math.atan2(
                Mouse.getY() - DisplayManager.HEIGHT / 2f, Mouse.getX() - DisplayManager.WIDTH / 2f));

        updateSelectedCell();

        // Acciones
        boolean button0 = Mouse.isButtonDown(0);
        if (button0 && !wasButton0) clickStartedOnUi = mouseOnUi;
        if (button0 && !clickStartedOnUi) primaryAction(!wasButton0);
        wasButton0 = button0;

        boolean button1 = Mouse.isButtonDown(1);
        if (button1 && !wasButton1) secondaryAction();
        wasButton1 = button1;

        boolean q = Keyboard.isKeyDown(Keyboard.KEY_Q);
        if (q && !wasQ) dropSelectedItem();
        wasQ = q;

        boolean e = Keyboard.isKeyDown(Keyboard.KEY_E);
        if (e && !wasE) pickUpItem();
        wasE = e;

        boolean b = Keyboard.isKeyDown(Keyboard.KEY_B);
        if (b && !wasB) buildMode = !buildMode;
        wasB = b;

        if (buildMode) updateBuildPosition();
        checkCollisions();

        if (hp <= 0) {
            die();
            respawn();
        }
    }

    private static void updateSelectedCell() {
        if (Keyboard.isKeyDown(Keyboard.KEY_1)) selectedCell = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_2)) selectedCell = 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_3)) selectedCell = 2;
        if (Keyboard.isKeyDown(Keyboard.KEY_4)) selectedCell = 3;

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
        float s = Struct.SIZE / 2;
        boolean overlapX = w.x() + s > x - half && w.x() - s < x + half;
        boolean overlapY = w.y() + s > y - half && w.y() - s < y + half;
        if (overlapX && w.y() + s + WALL_MARGIN > y - half && w.y() - s + WALL_MARGIN < y - half) collUp = true;
        if (overlapX && w.y() + s - WALL_MARGIN > y + half && w.y() - s - WALL_MARGIN < y + half) collDown = true;
        if (overlapY && w.x() + s + WALL_MARGIN > x - half && w.x() - s + WALL_MARGIN < x - half) collLeft = true;
        if (overlapY && w.x() + s - WALL_MARGIN > x + half && w.x() - s - WALL_MARGIN < x + half) collRight = true;
    }

    private static void primaryAction(boolean justPressed) {
        if (selectedItem() instanceof Gun gun) {
            double angle = Math.toRadians(-rotation + 90);
            float shotX = x + (float) Math.sin(angle) * HAND_OFFSET;
            float shotY = y + (float) Math.cos(angle) * HAND_OFFSET;
            gun.shoot(shotX, shotY, rotation, true, justPressed);
        }
    }

    private static void secondaryAction() {
        Item item = selectedItem();
        if (item == null) {
            return;
        }
        if (buildMode && canBuild && item.itemKind.isBuildable()) {
            WorldManager.build(new Struct(buildX, buildY, item.itemKind, item.cellTextureID));
        }
        useSelectedConsumable();
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
        buildX = cursorWorldX();
        buildY = cursorWorldY();
        canBuild = !WorldManager.isSolidAt(buildX, buildY)
                && distance(buildX, buildY, x, y) <= BUILD_RANGE;
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
