package structs;

import assets.TextureId;
import items.ItemKind;

/**
 * Estructura construida en el mundo, centrada en (x, y).
 *
 * <p>El mundo es una cuadrícula de casillas de {@link #SIZE} px; la casilla (i, j) está centrada en
 * (i * SIZE, j * SIZE). Los suelos ocupan una casilla entera. Los muros y medios muros son finos y van sobre
 * los bordes entre casillas: horizontales ({@code vertical = false}) o verticales. Miden una casilla de largo
 * más su grosor, para que dos muros que se juntan en una esquina se solapen y no quede hueco.
 */
public record Struct(float x, float y, ItemKind kind, TextureId texture, boolean vertical) {
    public static final float SIZE = 65;
    public static final float WALL_THICKNESS = 16;      // un cuarto de casilla
    public static final float HALF_WALL_THICKNESS = 13; // un quinto de casilla
    /** Mitad del lado más largo que puede tener una estructura (un muro con sus puntas). */
    public static final float MAX_HALF_EXTENT = (SIZE + WALL_THICKNESS) / 2;

    public Struct {
        if (!kind.isBuildable()) {
            throw new IllegalArgumentException("No es una estructura: " + kind);
        }
    }

    /** Suelo, o muro horizontal. */
    public Struct(float x, float y, ItemKind kind, TextureId texture) {
        this(x, y, kind, texture, false);
    }

    /** Muro horizontal en el borde norte de la casilla (tileX, tileY). */
    public static Struct northWall(int tileX, int tileY, ItemKind kind, TextureId texture) {
        return new Struct(tileX * SIZE, tileY * SIZE - SIZE / 2, kind, texture, false);
    }

    /** Muro vertical en el borde oeste de la casilla (tileX, tileY). */
    public static Struct westWall(int tileX, int tileY, ItemKind kind, TextureId texture) {
        return new Struct(tileX * SIZE - SIZE / 2, tileY * SIZE, kind, texture, true);
    }

    /**
     * La estructura que se construiría apuntando a (x, y): un suelo va a la casilla que hay debajo; un muro, al
     * borde más cercano, y su orientación sale de ese borde.
     */
    public static Struct snapped(float x, float y, ItemKind kind, TextureId texture) {
        float tileX = Math.round(x / SIZE) * SIZE;
        float tileY = Math.round(y / SIZE) * SIZE;
        if (kind == ItemKind.FLOOR) {
            return new Struct(tileX, tileY, kind, texture);
        }
        // Los bordes están a media casilla de los centros
        float lineX = (Math.round(x / SIZE - 0.5f) + 0.5f) * SIZE;
        float lineY = (Math.round(y / SIZE - 0.5f) + 0.5f) * SIZE;
        if (Math.abs(y - lineY) <= Math.abs(x - lineX)) {
            return new Struct(tileX, lineY, kind, texture, false);
        }
        return new Struct(lineX, tileY, kind, texture, true);
    }

    public boolean isWall() {
        return kind == ItemKind.WALL || kind == ItemKind.HALF_WALL;
    }

    public float thickness() {
        return switch (kind) {
            case WALL -> WALL_THICKNESS;
            case HALF_WALL -> HALF_WALL_THICKNESS;
            default -> SIZE;
        };
    }

    /** Mitad del ancho (eje x) de su caja. */
    public float halfWidth() {
        return (vertical ? thickness() : length()) / 2;
    }

    /** Mitad del alto (eje y) de su caja. */
    public float halfHeight() {
        return (vertical ? length() : thickness()) / 2;
    }

    private float length() {
        return isWall() ? SIZE + thickness() : SIZE;
    }

    /** {@code true} si su caja se solapa con la caja centrada en (x, y) de medio ancho halfW y medio alto halfH. */
    public boolean overlaps(float x, float y, float halfW, float halfH) {
        return Math.abs(x - this.x) < halfW + halfWidth() && Math.abs(y - this.y) < halfH + halfHeight();
    }

    /** Mismo sitio y orientación (para no construir dos muros en el mismo borde). */
    public boolean samePlaceAs(Struct other) {
        return Math.abs(x - other.x) < 1 && Math.abs(y - other.y) < 1 && vertical == other.vertical;
    }
}
