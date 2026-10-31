package world;

import static util.Geometry.NO_HIT;
import static util.Geometry.distance;
import static util.Geometry.isEarlierHit;
import static util.Geometry.segmentEntersBox;

import java.util.List;

import items.ItemStack;
import npc.Npc;
import structs.Struct;
import structs.StructKind;

/**
 * Las estructuras del mundo (suelos, muros, medios muros y contenedores): construirlas, derribarlas y preguntar si
 * algo choca con ellas. Cada estructura se guarda en su chunk (para dibujarla) y lo que bloquea el paso, además, en
 * rejillas de colisión, que son las que hacen rápidas las consultas.
 */
public final class Structures {
    /** Muros (no medios muros) indexados por posición, para que cada bala solo compruebe los cercanos. */
    private static final StructGrid bulletWalls = new StructGrid();
    /** Muros, medios muros y contenedores: bloquean el paso a jugador y NPC. */
    private static final StructGrid solids = new StructGrid();
    /** Distancia al centro de un contenedor roto a la que queda lo que tenía (en un anillo, y la mitad más lejos). */
    static final float SCATTER_RADIUS = 30;
    /** Ángulo del primer montón al esparcir: hacia abajo a la derecha, para que no salgan todos en fila. */
    private static final double SCATTER_START_ANGLE = Math.PI / 4;

    private Structures() {
    }

    /** Única forma de añadir estructuras al mundo: así las rejillas de colisión quedan sincronizadas. */
    public static void build(Struct struct) {
        WorldManager.closestChunk(struct.x(), struct.y()).add(struct);
        if (struct.kind().blocksBullets) {
            bulletWalls.add(struct);
        }
        if (struct.kind().solid) {
            solids.add(struct);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Contenedores
    // ---------------------------------------------------------------------------------------------

    /** Construye un contenedor (p.ej. {@code Struct.onTile(x, y, StructKind.CRATE)}) y lo devuelve, para llenarlo. */
    public static Container buildContainer(Struct struct) {
        if (struct.kind() != StructKind.CRATE) {
            throw new IllegalArgumentException("No es un contenedor: " + struct.kind());
        }
        build(struct);
        return WorldManager.closestChunk(struct.x(), struct.y()).containers.getLast();
    }

    /** El contenedor de la casilla en la que cae (x, y), o {@code null}. */
    public static Container containerAt(float x, float y) {
        Struct tile = Struct.snapped(x, y, StructKind.CRATE, null);
        for (Chunk chunk : WorldManager.chunks) {
            for (Container container : chunk.containers) {
                if (container.struct.samePlaceAs(tile)) {
                    return container;
                }
            }
        }
        return null;
    }

    /** El contenedor más cercano a (x, y), a menos de {@code range} de su centro, o {@code null}. */
    public static Container nearestContainer(float x, float y, float range) {
        Container nearest = null;
        float best = range;
        for (Chunk chunk : WorldManager.chunks) {
            for (Container container : chunk.containers) {
                float d = distance(x, y, container.struct.x(), container.struct.y());
                if (d < best) {
                    best = d;
                    nearest = container;
                }
            }
        }
        return nearest;
    }

    /**
     * Rompe un contenedor: deja de chocar y de dibujarse, y lo que tenía queda esparcido por el suelo alrededor de
     * donde estaba ({@link #scatter}).
     */
    public static void remove(Container container) {
        Struct struct = container.struct;
        solids.removeIf(struct.x(), struct.y(), s -> s == struct);
        for (Chunk chunk : WorldManager.chunks) {
            chunk.containers.remove(container);
        }
        container.markRemoved();
        scatter(container.inventory.removeAll(), struct.x(), struct.y());
    }

    /**
     * Deja cada pila en su propio montón, repartidas en círculo alrededor de (x, y): así no queda todo amontonado
     * en un punto y se ve qué había.
     */
    static void scatter(List<ItemStack> stacks, float x, float y) {
        for (int i = 0; i < stacks.size(); i++) {
            double angle = 2 * Math.PI * i / stacks.size() + SCATTER_START_ANGLE;
            float radius = SCATTER_RADIUS + (i % 2) * SCATTER_RADIUS / 2; // dos anillos, para que no se pisen
            WorldManager.dropItem(stacks.get(i), x + (float) Math.cos(angle) * radius,
                    y + (float) Math.sin(angle) * radius);
        }
    }

    /**
     * Derriba el muro o medio muro que haya en el mismo borde que {@code edge}.
     *
     * @return qué había ({@link StructKind#WALL} o {@link StructKind#HALF_WALL}), o {@code null} si no había nada
     */
    public static StructKind removeWallAt(Struct edge) {
        StructKind[] removed = {null};
        solids.forEachNear(edge.x(), edge.y(), 0, s -> {
            if (s.samePlaceAs(edge)) removed[0] = s.kind();
        });
        if (removed[0] == null || !solids.removeIf(edge.x(), edge.y(), s -> s.samePlaceAs(edge))) {
            return null;
        }
        bulletWalls.removeIf(edge.x(), edge.y(), s -> s.samePlaceAs(edge));
        for (Chunk chunk : WorldManager.chunks) {
            chunk.walls.removeIf(s -> s.samePlaceAs(edge));
            chunk.halfWalls.removeIf(s -> s.samePlaceAs(edge));
        }
        return removed[0];
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
        for (Chunk chunk : WorldManager.chunks) {
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

    // ---------------------------------------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------------------------------------

    /** {@code true} si un NPC en (x, y) chocaría con un muro o medio muro. */
    public static boolean isSolidAt(float x, float y) {
        return isBlocked(x, y, Npc.BODY_HALF, Npc.BODY_HALF);
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

    /** {@code true} si un muro (no medio muro) corta el tramo (x0, y0) → (x1, y1): las balas no pasarían. */
    public static boolean isWallBetween(float x0, float y0, float x1, float y1) {
        return firstWallHit(x0, y0, x1, y1) != NO_HIT;
    }

    /**
     * Primer muro (no medio muro) que cruza el tramo (x0, y0) → (x1, y1), como fracción del tramo (ver
     * {@link util.Geometry#segmentEntersBox}), o {@code NO_HIT}.
     */
    static float firstWallHit(float x0, float y0, float x1, float y1) {
        float midX = (x0 + x1) / 2, midY = (y0 + y1) / 2;
        float range = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 2;
        float[] first = {NO_HIT};
        bulletWalls.forEachNear(midX, midY, range, wall -> {
            float t = segmentEntersBox(x0, y0, x1, y1, wall.x(), wall.y(), wall.halfWidth(), wall.halfHeight());
            if (isEarlierHit(t, first[0])) {
                first[0] = t;
            }
        });
        return first[0];
    }

    /** Olvida todas las estructuras (las de los chunks se van con ellos en {@link WorldManager#reset()}). */
    static void reset() {
        bulletWalls.clear();
        solids.clear();
    }
}
