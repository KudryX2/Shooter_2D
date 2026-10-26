package world;

/**
 * Fogonazo de un disparo en (x, y) con la misma rotación que el proyectil. Solo es visual.
 *
 * @param byPlayer el del jugador se dibuja pegado a su arma aunque se mueva
 */
public record MuzzleFlash(float x, float y, float rotation, long time, boolean byPlayer) {
    /** Duración del fogonazo en milisegundos. */
    public static final long DURATION_MS = 60;
}
