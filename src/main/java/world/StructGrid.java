package world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import structs.Struct;

/**
 * Rejilla espacial de estructuras: cada una se guarda en la celda (de {@link Struct#SIZE} px) donde está su
 * centro. Una consulta solo mira las celdas que cubre la caja buscada, ampliada con el tamaño máximo de una
 * estructura, así que el coste depende de lo que hay cerca y no del número total de estructuras del mundo.
 */
final class StructGrid {
    private static final float CELL = Struct.SIZE;

    private final Map<Long, List<Struct>> cells = new HashMap<>();

    void add(Struct struct) {
        cells.computeIfAbsent(key(cell(struct.x()), cell(struct.y())), k -> new ArrayList<>()).add(struct);
    }

    void clear() {
        cells.clear();
    }

    /** Quita las estructuras cercanas a (x, y) que cumplan {@code filter}. Devuelve {@code true} si quitó alguna. */
    boolean removeIf(float x, float y, Predicate<Struct> filter) {
        boolean removed = false;
        int x0 = cell(x - Struct.MAX_HALF_EXTENT), x1 = cell(x + Struct.MAX_HALF_EXTENT);
        int y0 = cell(y - Struct.MAX_HALF_EXTENT), y1 = cell(y + Struct.MAX_HALF_EXTENT);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cy = y0; cy <= y1; cy++) {
                List<Struct> structs = cells.get(key(cx, cy));
                if (structs != null) {
                    removed |= structs.removeIf(filter);
                }
            }
        }
        return removed;
    }

    /** {@code true} si alguna estructura se solapa con la caja centrada en (x, y) de medio ancho/alto dados. */
    boolean anyOverlapping(float x, float y, float halfW, float halfH) {
        boolean[] found = {false};
        forEachNear(x, y, Math.max(halfW, halfH), s -> {
            if (!found[0] && s.overlaps(x, y, halfW, halfH)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /**
     * Pasa a {@code action} todas las estructuras que tocan la caja [x-range, x+range] × [y-range, y+range], y
     * quizá alguna algo más lejana (las celdas son enteras): {@code action} debe hacer su propia comprobación.
     */
    void forEachNear(float x, float y, float range, Consumer<Struct> action) {
        float reach = range + Struct.MAX_HALF_EXTENT;
        int x0 = cell(x - reach), x1 = cell(x + reach);
        int y0 = cell(y - reach), y1 = cell(y + reach);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cy = y0; cy <= y1; cy++) {
                List<Struct> structs = cells.get(key(cx, cy));
                if (structs != null) {
                    structs.forEach(action);
                }
            }
        }
    }

    private static int cell(float coordinate) {
        return (int) Math.floor(coordinate / CELL);
    }

    private static long key(int cx, int cy) {
        return ((long) cx << 32) ^ (cy & 0xFFFF_FFFFL);
    }
}
