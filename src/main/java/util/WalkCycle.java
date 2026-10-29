package util;

/**
 * Cuánto ha andado un personaje y si está andando ahora mismo. Solo sirve para dibujarlo (la animación de andar
 * avanza con la distancia recorrida); no afecta a la partida.
 */
public final class WalkCycle {
    /** Tras este tiempo sin dar un paso se le considera quieto (así un frame sin moverse no corta la animación). */
    private static final long STILL_AFTER_MS = 100;

    private float distance;
    private long lastStep = GameClock.NEVER;

    /** Ha avanzado {@code distance} unidades (nada si es 0, p.ej. si un muro se lo ha impedido). */
    public void step(float distance) {
        if (distance > 0) {
            this.distance += distance;
            lastStep = GameClock.now();
        }
    }

    public boolean isWalking() {
        return GameClock.since(lastStep) < STILL_AFTER_MS;
    }

    /** Distancia total andada. */
    public float distance() {
        return distance;
    }
}
