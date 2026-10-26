package world;

import static util.Geometry.overlaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import structs.Struct;

/**
 * Rejilla espacial de estructuras: cada una se guarda en la celda (de {@link Struct#SIZE} px) donde está su
 * centro. Una consulta de radio r solo mira las celdas que cubre la caja [x-r, x+r], así que el coste depende
 * de lo que hay cerca y no del número total de estructuras del mundo.
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

    /** {@code true} si alguna estructura tiene su centro a menos de {@code halfSize} de (x, y) en ambos ejes. */
    boolean anyOverlapping(float x, float y, float halfSize) {
        int x0 = cell(x - halfSize), x1 = cell(x + halfSize);
        int y0 = cell(y - halfSize), y1 = cell(y + halfSize);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cy = y0; cy <= y1; cy++) {
                List<Struct> structs = cells.get(key(cx, cy));
                if (structs == null) {
                    continue;
                }
                for (Struct s : structs) {
                    if (overlaps(x, y, s.x(), s.y(), halfSize)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Pasa a {@code action} las estructuras de las celdas que cubre la caja [x-range, x+range]. Puede incluir
     * alguna algo más lejana (las celdas son enteras): {@code action} debe hacer su propia comprobación.
     */
    void forEachNear(float x, float y, float range, Consumer<Struct> action) {
        int x0 = cell(x - range), x1 = cell(x + range);
        int y0 = cell(y - range), y1 = cell(y + range);
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
