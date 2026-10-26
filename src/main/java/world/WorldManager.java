package world;

import static util.Geometry.distance;
import static util.Geometry.overlaps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import graphics.DisplayManager;
import items.DroppedItem;
import items.Item;
import items.ItemKind;
import items.Projectile;
import npc.Npc;
import player.Player;
import structs.Struct;
import util.GameClock;

public final class WorldManager {
    private static final float PROJECTILE_HITBOX = 20;
    private static final float WALL_HITBOX = 30;

    public static final List<Chunk> chunks = new ArrayList<>();
    public static final List<Chunk> activeChunks = new ArrayList<>();
    public static final List<Projectile> projectiles = new ArrayList<>();
    public static final List<Npc> npcs = new ArrayList<>();

    public static final List<MuzzleFlash> muzzleFlashes = new ArrayList<>();

    /** Muros (no medios muros) indexados por posición, para que cada bala solo compruebe los cercanos. */
    private static final StructGrid bulletWalls = new StructGrid();
    /** Muros y medios muros: bloquean el paso a jugador y NPC ({@link #isSolidAt}). */
    private static final StructGrid solids = new StructGrid();

    public static int zombiesKilled;

    private WorldManager() {
    }

    public static void update() {
        projectiles.removeIf(WorldManager::updateProjectile);
        muzzleFlashes.removeIf(f -> GameClock.since(f.time()) > MuzzleFlash.DURATION_MS);
        updateNpcs();
        updateChunks();
    }

    /** Mueve el proyectil y resuelve sus impactos. Devuelve {@code true} si hay que eliminarlo. */
    private static boolean updateProjectile(Projectile p) {
        p.move();
        if (p.isOutOfRange()) {
            return true;
        }

        if (overlaps(p.x, p.y, Player.x, Player.y, PROJECTILE_HITBOX)) {
            Player.hp -= p.damage / 10;
            return true;
        }

        for (Npc npc : npcs) {
            if (npc.alive && overlaps(p.x, p.y, npc.x, npc.y, PROJECTILE_HITBOX)) {
                // Headshot: solo balas del jugador, y solo si estaba apuntando justo al objetivo
                if (p.byPlayer && overlaps(p.x, p.y, Player.aimX, Player.aimY, PROJECTILE_HITBOX)) {
                    npc.die(true);
                } else {
                    npc.takeDamage(p.damage);
                }
                return true;
            }
        }

        return bulletWalls.anyOverlapping(p.x, p.y, WALL_HITBOX);
    }

    private static void updateNpcs() {
        for (Npc npc : npcs) {
            npc.update();
        }
        npcs.removeIf(npc -> npc.canDisappear);
    }

    private static void updateChunks() {
        activeChunks.clear();
        for (Chunk chunk : chunks) {
            if (distance(chunk.x, chunk.y, Player.x, Player.y) < DisplayManager.WIDTH * 2) {
                activeChunks.add(chunk);
            }
        }
    }

    /** {@code true} si hay un muro o medio muro bloqueando el punto (x, y). */
    public static boolean isSolidAt(float x, float y) {
        return solids.anyOverlapping(x, y, Struct.SIZE);
    }

    /**
     * Pasa a {@code action} los muros y medios muros a unos {@code range} px de (x, y), y quizá alguno algo más
     * lejano: {@code action} debe hacer su propia comprobación de distancia.
     */
    public static void forEachSolidNear(float x, float y, float range, Consumer<Struct> action) {
        solids.forEachNear(x, y, range, action);
    }

    /** Única forma de añadir estructuras al mundo: así la rejilla de colisiones queda sincronizada. */
    public static void build(Struct struct) {
        closestChunk(struct.x(), struct.y()).add(struct);
        if (struct.kind() == ItemKind.WALL) {
            bulletWalls.add(struct);
        }
        if (struct.kind() == ItemKind.WALL || struct.kind() == ItemKind.HALF_WALL) {
            solids.add(struct);
        }
    }

    /** Vacía el mundo por completo (chunks, NPCs, proyectiles, efectos, rejilla de colisiones y contador). */
    public static void reset() {
        chunks.clear();
        activeChunks.clear();
        projectiles.clear();
        npcs.clear();
        muzzleFlashes.clear();
        bulletWalls.clear();
        solids.clear();
        zombiesKilled = 0;
    }

    public static void dropItem(Item item, float x, float y) {
        closestChunk(x, y).droppedItems.add(new DroppedItem(item, x, y));
    }

    private static Chunk closestChunk(float x, float y) {
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
