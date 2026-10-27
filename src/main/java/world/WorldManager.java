package world;

import static util.Geometry.NO_HIT;
import static util.Geometry.distance;
import static util.Geometry.segmentEntersBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import graphics.DisplayManager;
import items.DroppedItem;
import items.Item;
import items.Projectile;
import npc.Npc;
import player.Player;
import structs.Struct;
import structs.StructKind;
import util.GameClock;

public final class WorldManager {
    private static final float PROJECTILE_HITBOX = 20;
    /** Medio lado de la caja con la que chocan los NPC contra los muros (su cuerpo visible mide ~22 px de radio). */
    public static final float CHARACTER_HALF = 20;

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

    /**
     * Mueve el proyectil y resuelve sus impactos. Devuelve {@code true} si hay que eliminarlo.
     *
     * <p>Una bala avanza decenas de píxeles por frame, así que no basta con mirar dónde acaba: se comprueba todo
     * el tramo recorrido y gana el primer obstáculo (muro, NPC o jugador). Así no atraviesa muros finos ni se
     * salta objetivos, y un muro protege de verdad a quien está detrás.
     */
    private static boolean updateProjectile(Projectile p) {
        float x0 = p.x, y0 = p.y;
        p.move();
        float x1 = p.x, y1 = p.y;

        float first = firstWallHit(x0, y0, x1, y1);
        boolean hitsPlayer = false;
        Npc hitNpc = null;

        float t = segmentEntersBox(x0, y0, x1, y1, Player.x, Player.y, PROJECTILE_HITBOX, PROJECTILE_HITBOX);
        if (isCloser(t, first)) {
            first = t;
            hitsPlayer = true;
        }
        for (Npc npc : npcs) {
            if (!npc.alive) {
                continue;
            }
            t = segmentEntersBox(x0, y0, x1, y1, npc.x, npc.y, PROJECTILE_HITBOX, PROJECTILE_HITBOX);
            if (isCloser(t, first)) {
                first = t;
                hitsPlayer = false;
                hitNpc = npc;
            }
        }

        if (hitNpc != null) {
            // Headshot: solo balas del jugador, y solo si pasan justo por donde apuntaba
            boolean throughAim = segmentEntersBox(x0, y0, x1, y1, Player.aimX, Player.aimY,
                    PROJECTILE_HITBOX, PROJECTILE_HITBOX) != NO_HIT;
            if (p.byPlayer && throughAim) {
                hitNpc.die(true);
            } else {
                hitNpc.takeDamage(p.damage);
            }
            return true;
        }
        if (hitsPlayer) {
            Player.hp -= p.damage / 10;
            return true;
        }
        return first != NO_HIT || p.isOutOfRange();
    }

    private static boolean isCloser(float t, float best) {
        return t != NO_HIT && (best == NO_HIT || t < best);
    }

    /** {@code true} si un muro (no medio muro) corta el tramo (x0, y0) → (x1, y1): las balas no pasarían. */
    public static boolean isWallBetween(float x0, float y0, float x1, float y1) {
        return firstWallHit(x0, y0, x1, y1) != NO_HIT;
    }

    /** Primer muro (no medio muro) que cruza el tramo (x0, y0) → (x1, y1), como fracción del tramo, o NO_HIT. */
    private static float firstWallHit(float x0, float y0, float x1, float y1) {
        float midX = (x0 + x1) / 2, midY = (y0 + y1) / 2;
        float range = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 2;
        float[] first = {NO_HIT};
        bulletWalls.forEachNear(midX, midY, range, wall -> {
            float t = segmentEntersBox(x0, y0, x1, y1, wall.x(), wall.y(), wall.halfWidth(), wall.halfHeight());
            if (isCloser(t, first[0])) {
                first[0] = t;
            }
        });
        return first[0];
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

    /** {@code true} si un personaje (NPC) en (x, y) chocaría con un muro o medio muro. */
    public static boolean isSolidAt(float x, float y) {
        return isBlocked(x, y, CHARACTER_HALF, CHARACTER_HALF);
    }

    /** {@code true} si la caja centrada en (x, y) con medio ancho y alto dados choca con un muro o medio muro. */
    public static boolean isBlocked(float x, float y, float halfW, float halfH) {
        return solids.anyOverlapping(x, y, halfW, halfH);
    }

    /** {@code true} si ya hay un muro o medio muro en ese mismo borde. */
    public static boolean hasWallAt(Struct wall) {
        boolean[] found = {false};
        solids.forEachNear(wall.x(), wall.y(), 0, s -> found[0] |= s.samePlaceAs(wall));
        return found[0];
    }

    /**
     * Pasa a {@code action} los muros y medios muros que tocan la caja de medio lado {@code range} alrededor de
     * (x, y), y quizá alguno algo más lejano: {@code action} debe hacer su propia comprobación.
     */
    public static void forEachSolidNear(float x, float y, float range, Consumer<Struct> action) {
        solids.forEachNear(x, y, range, action);
    }

    /** Única forma de añadir estructuras al mundo: así la rejilla de colisiones queda sincronizada. */
    public static void build(Struct struct) {
        closestChunk(struct.x(), struct.y()).add(struct);
        if (struct.kind().blocksBullets) {
            bulletWalls.add(struct);
        }
        if (struct.isWall()) {
            solids.add(struct);
        }
    }

    /**
     * Derriba el muro o medio muro que haya en el mismo borde que {@code edge}.
     *
     * @return {@code true} si había alguno
     */
    public static boolean removeWallAt(Struct edge) {
        if (!solids.removeIf(edge.x(), edge.y(), s -> s.samePlaceAs(edge))) {
            return false;
        }
        bulletWalls.removeIf(edge.x(), edge.y(), s -> s.samePlaceAs(edge));
        for (Chunk chunk : chunks) {
            chunk.walls.removeIf(s -> s.samePlaceAs(edge));
            chunk.halfWalls.removeIf(s -> s.samePlaceAs(edge));
        }
        return true;
    }

    /**
     * Quita el suelo de más arriba en la casilla centrada en (x, y), pero solo si debajo queda otro: así se
     * puede quitar un suelo construido sin dejar un agujero en el del mapa.
     *
     * @return {@code true} si se ha quitado alguno
     */
    public static boolean removeBuiltFloorAt(float x, float y) {
        Struct tile = new Struct(x, y, StructKind.FLOOR, null);
        Chunk top = null;
        int count = 0;
        for (Chunk chunk : chunks) {
            for (Struct floor : chunk.floors) {
                if (floor.samePlaceAs(tile)) {
                    count++;
                    top = chunk;
                }
            }
        }
        if (count < 2) {
            return false;
        }
        List<Struct> floors = top.floors;
        for (int i = floors.size() - 1; i >= 0; i--) {
            if (floors.get(i).samePlaceAs(tile)) {
                floors.remove(i);
                return true;
            }
        }
        return false;
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
