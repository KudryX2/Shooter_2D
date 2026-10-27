package world;

/** Reglas de movimiento contra muros, comunes a jugador y NPC. */
public final class Movement {

    private Movement() {
    }

    /**
     * {@code true} si un cuerpo (caja de medio lado {@code half}) puede pasar de (fromX, fromY) a (toX, toY) sin
     * meterse en un muro. Si ya está dentro de uno (p.ej. le han construido encima) puede moverse para salir.
     */
    public static boolean canMove(float fromX, float fromY, float toX, float toY, float half) {
        return !Structures.isBlocked(toX, toY, half, half) || Structures.isBlocked(fromX, fromY, half, half);
    }
}
