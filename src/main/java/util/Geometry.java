package util;

/** Utilidades geométricas compartidas. */
public final class Geometry {

    private Geometry() {
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** {@code true} si los dos puntos están dentro de una caja de {@code halfSize} de lado medio. */
    public static boolean overlaps(float ax, float ay, float bx, float by, float halfSize) {
        return Math.abs(ax - bx) < halfSize && Math.abs(ay - by) < halfSize;
    }
}
