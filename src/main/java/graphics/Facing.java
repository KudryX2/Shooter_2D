package graphics;

/**
 * Hacia dónde mira un personaje en pantalla. El orden es el de {@link assets.TextureId#DIRECTIONS} (el ordinal
 * sirve de índice).
 */
enum Facing {
    DOWN, UP, LEFT, RIGHT;

    /** A partir de un ángulo en grados (0 = derecha, 90 = abajo, en sentido horario, como {@link util.Geometry}). */
    static Facing of(float rotation) {
        float angle = ((rotation % 360) + 360) % 360;
        if (angle < 45 || angle >= 315) {
            return RIGHT;
        }
        if (angle < 135) {
            return DOWN;
        }
        if (angle < 225) {
            return LEFT;
        }
        return UP;
    }
}
