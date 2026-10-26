package util;

/**
 * Reloj único del juego. Todo lo que dependa del tiempo (movimiento, cadencias, animaciones, temporizadores)
 * debe usarlo en lugar de {@code System.currentTimeMillis()}: así la pausa lo congela todo a la vez y los tests
 * pueden avanzar el tiempo sin esperar.
 *
 * <p>La ventana llama a {@link #tick(float)} una vez por frame con el tiempo real transcurrido.
 */
public final class GameClock {
    /** Valor inicial para "la última vez que pasó X": queda infinitamente lejos en el pasado. */
    public static final long NEVER = Long.MIN_VALUE / 4;

    private static double time;
    private static float delta;
    private static boolean paused;

    private GameClock() {
    }

    /** Avanza el reloj {@code deltaMs} milisegundos (nada si está en pausa). */
    public static void tick(float deltaMs) {
        delta = paused ? 0 : deltaMs;
        time += delta;
    }

    /** Milisegundos de juego del frame actual (0 en pausa). */
    public static float delta() {
        return delta;
    }

    /** Milisegundos de juego transcurridos desde el inicio (no avanza en pausa). */
    public static long now() {
        return (long) time;
    }

    /** Milisegundos de juego transcurridos desde {@code moment}. */
    public static long since(long moment) {
        return now() - moment;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void togglePause() {
        paused = !paused;
        if (paused) {
            delta = 0;
        }
    }

    /** Vuelve al instante 0, sin pausa. */
    public static void reset() {
        time = 0;
        delta = 0;
        paused = false;
    }
}
