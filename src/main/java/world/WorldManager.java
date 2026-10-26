package world;

import static util.Geometry.distance;
import static util.Geometry.overlaps;

import java.util.ArrayList;
import java.util.List;

import graphics.DisplayManager;
import items.DroppedItem;
import items.Item;
import items.Projectile;
import npc.Npc;
import player.Player;
import structs.Struct;

public final class WorldManager {
    private static final float PROJECTILE_HITBOX = 20;
    private static final float WALL_HITBOX = 30;

    public static final List<Chunk> chunks = new ArrayList<>();
    public static final List<Chunk> activeChunks = new ArrayList<>();
    public static final List<Projectile> projectiles = new ArrayList<>();
    public static final List<Npc> npcs = new ArrayList<>();

    public static int zombiesKilled;

    private WorldManager() {
    }

    public static void update() {
        projectiles.removeIf(WorldManager::updateProjectile);
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
                // Headshot si el cursor del jugador está encima del objetivo
                if (overlaps(p.x, p.y, Player.cursorWorldX(), Player.cursorWorldY(), PROJECTILE_HITBOX)) {
                    npc.die(true);
                } else {
                    npc.hp -= p.damage;
                }
                return true;
            }
        }

        for (Chunk chunk : chunks) {
            for (Struct wall : chunk.walls) {
                if (overlaps(p.x, p.y, wall.x(), wall.y(), WALL_HITBOX)) {
                    return true;
                }
            }
        }
        return false;
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
        for (Chunk chunk : chunks) {
            for (List<Struct> structs : List.of(chunk.walls, chunk.halfWalls)) {
                for (Struct s : structs) {
                    if (overlaps(s.x(), s.y(), x, y, Struct.SIZE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void build(Struct struct) {
        closestChunk(struct.x(), struct.y()).add(struct);
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
