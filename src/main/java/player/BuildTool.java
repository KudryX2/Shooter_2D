package player;

import static util.Geometry.distance;

import items.Item;
import items.StructItem;
import structs.Struct;
import structs.StructKind;
import world.Container;
import world.Structures;

/**
 * Construir y derribar con un {@link StructItem} en la mano. Un muro va al borde de casilla más cercano al punto
 * apuntado y un suelo a la casilla de debajo; se ve antes una vista previa ({@link #preview()}).
 */
public final class BuildTool {
    /** Distancia máxima desde el jugador a lo que se construye o derriba. */
    static final float RANGE = 250;

    private static Struct preview;
    private static boolean canBuild;

    private BuildTool() {
    }

    /** Lo que se construiría ahora (para la vista previa), o {@code null} si no hay nada construible en la mano. */
    public static Struct preview() {
        return preview;
    }

    /** Si la vista previa se puede construir (verde) o no (roja). */
    public static boolean canBuildPreview() {
        return canBuild;
    }

    /** Recalcula la vista previa para el objeto en la mano y el punto apuntado. Una vez por frame. */
    static void update(Item heldItem, float aimX, float aimY) {
        if (!(heldItem instanceof StructItem item)) {
            preview = null;
            canBuild = false;
            return;
        }
        preview = Struct.snapped(aimX, aimY, item.kind, item.kind.texture);
        canBuild = canBuild(preview);
    }

    /**
     * Construye la vista previa, si se puede.
     *
     * @return {@code true} si ha construido (quien llama gasta el material)
     */
    static boolean build() {
        if (preview == null || !canBuild) {
            return false;
        }
        Structures.build(preview);
        canBuild = false; // ese borde ya está ocupado hasta el siguiente frame
        return true;
    }

    /**
     * Se puede construir al alcance; lo que bloquea el paso (un muro o una caja), solo en un sitio libre y sin quedar
     * encima del jugador. Si deja huecos con los muros vecinos es cosa del jugador.
     */
    static boolean canBuild(Struct struct) {
        if (distance(struct.x(), struct.y(), Player.x, Player.y) > RANGE) {
            return false;
        }
        if (!struct.kind().solid) {
            return true;
        }
        float half = Player.SIZE / 2;
        return !Structures.hasWallAt(struct) && !struct.overlaps(Player.x, Player.y, half, half);
    }

    /**
     * Derriba, si está al alcance, la caja de la casilla de (wx, wy) (lo que tuviera dentro queda esparcido); si
     * no hay, el muro del borde más cercano (el mismo que marcaría la vista previa), y si tampoco, un suelo
     * construido en esa casilla.
     *
     * @return qué ha derribado (para devolver el material), o {@code null} si nada
     */
    static StructKind demolish(float wx, float wy) {
        Container crate = Structures.containerAt(wx, wy);
        if (crate != null && inRange(crate.struct)) {
            Structures.remove(crate);
            return crate.struct.kind();
        }
        Struct edge = Struct.snapped(wx, wy, StructKind.WALL, null);
        if (inRange(edge)) {
            StructKind wall = Structures.removeWallAt(edge);
            if (wall != null) {
                return wall;
            }
        }
        Struct tile = Struct.snapped(wx, wy, StructKind.FLOOR, null);
        return inRange(tile) && Structures.removeBuiltFloorAt(tile.x(), tile.y()) ? StructKind.FLOOR : null;
    }

    private static boolean inRange(Struct struct) {
        return distance(struct.x(), struct.y(), Player.x, Player.y) <= RANGE;
    }
}
