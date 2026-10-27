package world;

import static util.Geometry.distance;

import java.util.ArrayList;
import java.util.List;

import graphics.DisplayManager;
import items.DroppedItem;
import items.Item;
import items.Projectile;
import npc.Npc;
import player.Player;
import util.GameClock;

/**
 * El contenido del mundo y su actualización por frame. Las estructuras y sus colisiones están en
 * {@link Structures}; el movimiento e impactos de las balas, en {@link Projectiles}.
 */
public final class WorldManager {
    public static final List<Chunk> chunks = new ArrayList<>();
    /** Chunks cerca del jugador: los únicos que se dibujan y donde se pueden coger objetos. */
    public static final List<Chunk> activeChunks = new ArrayList<>();
    public static final List<Projectile> projectiles = new ArrayList<>();
    public static final List<Npc> npcs = new ArrayList<>();
    public static final List<MuzzleFlash> muzzleFlashes = new ArrayList<>();

    public static int zombiesKilled;

    private WorldManager() {
    }

    public static void update() {
        Projectiles.update();
        muzzleFlashes.removeIf(f -> GameClock.since(f.time()) > MuzzleFlash.DURATION_MS);
        updateNpcs();
        updateChunks();
    }

    private static void updateNpcs() {
        for (Npc npc : npcs) {
            npc.update();
        }
        npcs.removeIf(Npc::canDisappear);
    }

    private static void updateChunks() {
        activeChunks.clear();
        for (Chunk chunk : chunks) {
            if (distance(chunk.x, chunk.y, Player.x, Player.y) < DisplayManager.WIDTH * 2) {
                activeChunks.add(chunk);
            }
        }
    }

    public static void dropItem(Item item, float x, float y) {
        closestChunk(x, y).droppedItems.add(new DroppedItem(item, x, y));
    }

    /** Vacía el mundo por completo (chunks, estructuras, NPCs, proyectiles, efectos y contador). */
    public static void reset() {
        chunks.clear();
        activeChunks.clear();
        projectiles.clear();
        npcs.clear();
        muzzleFlashes.clear();
        Structures.reset();
        zombiesKilled = 0;
    }

    static Chunk closestChunk(float x, float y) {
        if (chunks.isEmpty()) {
            throw new IllegalStateException("El mundo no tiene chunks");
        }
        Chunk closest = chunks.getFirst();
        float best = Float.MAX_VALUE;
        for (Chunk chunk : chunks) {
            float d = distance(chunk.x, chunk.y, x, y);
            if (d < best) {
                best = d;
                closest = chunk;
            }
        }
        return closest;
    }
}
