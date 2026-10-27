package util;

/**
 * Utilidades geométricas compartidas.
 *
 * <p>Ángulos: en grados y en coordenadas de pantalla (y hacia abajo): 0 = este, 90 = sur, 180 = oeste,
 * 270 = norte. Jugador, NPC y balas usan todos esta convención.
 */
public final class Geometry {
    /** Resultado de {@link #segmentEntersBox} cuando el segmento no toca la caja. */
    public static final float NO_HIT = -1;

    private Geometry() {
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Ángulo desde (fromX, fromY) hacia (toX, toY). */
    public static float angleTo(float fromX, float fromY, float toX, float toY) {
        return (float) Math.toDegrees(Math.atan2(toY - fromY, toX - fromX));
    }

    /** Componente x de un paso de longitud 1 en la dirección {@code angle}. */
    public static float dirX(float angle) {
        return (float) Math.cos(Math.toRadians(angle));
    }

    /** Componente y de un paso de longitud 1 en la dirección {@code angle}. */
    public static float dirY(float angle) {
        return (float) Math.sin(Math.toRadians(angle));
    }

    /** {@code true} si los dos puntos están dentro de una caja de {@code halfSize} de lado medio. */
    public static boolean overlaps(float ax, float ay, float bx, float by, float halfSize) {
        return Math.abs(ax - bx) < halfSize && Math.abs(ay - by) < halfSize;
    }

    /**
     * Dónde entra el segmento (x0, y0) → (x1, y1) en la caja centrada en (cx, cy) con medio ancho {@code halfW}
     * y medio alto {@code halfH}: una fracción entre 0 (inicio) y 1 (final), 0 si ya empieza dentro, o
     * {@link #NO_HIT} si no la toca.
     */
    public static float segmentEntersBox(float x0, float y0, float x1, float y1,
                                         float cx, float cy, float halfW, float halfH) {
        float[] range = {0, 1};
        if (!clipAxis(x0, x1 - x0, cx - halfW, cx + halfW, range)
                || !clipAxis(y0, y1 - y0, cy - halfH, cy + halfH, range)) {
            return NO_HIT;
        }
        return range[0];
    }

    /** {@code true} si el impacto {@code t} (de {@link #segmentEntersBox}) ocurre antes que {@code best}. */
    public static boolean isEarlierHit(float t, float best) {
        return t != NO_HIT && (best == NO_HIT || t < best);
    }

    /** Recorta {@code range} (fracciones del segmento) al tramo en el que el eje queda entre min y max. */
    private static boolean clipAxis(float start, float delta, float min, float max, float[] range) {
        if (delta == 0) {
            return start > min && start < max;
        }
        float t1 = (min - start) / delta;
        float t2 = (max - start) / delta;
        range[0] = Math.max(range[0], Math.min(t1, t2));
        range[1] = Math.min(range[1], Math.max(t1, t2));
        return range[0] <= range[1];
    }
}
