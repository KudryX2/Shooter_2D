package graphics;

import com.badlogic.gdx.Gdx;

import util.GameClock;

/** Tamaño de la pantalla, zoom del mundo y paso del tiempo real al reloj del juego. */
public final class DisplayManager {
    public static final String TITLE = "Too Late To Retrieve 2D";
    public static final int FPS = 120;
    /** Tamaño de la ventana (si la pantalla es más pequeña, se reduce para que quepa). */
    public static final int WINDOWED_WIDTH = 1600;
    public static final int WINDOWED_HEIGHT = 900;
    /** Unidades del mundo por píxel del pixel art (una casilla de 64 son 16 px). */
    public static final float ART_PIXEL = 4;
    /**
     * {@code true}: el mundo se dibuja a la resolución del pixel art y se amplía a la pantalla un número entero de
     * veces, así que todos los píxeles del dibujo salen iguales y no tiemblan al moverse. {@code false}: se dibuja
     * directamente a resolución de pantalla con {@link #SMOOTH_ZOOM} (como antes); por si hubiera algún problema.
     */
    public static final boolean PIXEL_PERFECT = true;
    /** Zoom sin {@link #PIXEL_PERFECT}: 1.25 = todo se ve al 80 % y cabe más mapa. */
    private static final float SMOOTH_ZOOM = 1.25f;
    /** Cuánto mundo se quiere ver en vertical (lo mismo que con {@link #SMOOTH_ZOOM} en la ventana de 900 px). */
    private static final float TARGET_VIEW_HEIGHT = WINDOWED_HEIGHT * SMOOTH_ZOOM;
    /** Límite del delta (ms) para evitar saltos enormes tras una pausa (carga, ventana arrastrada...). */
    private static final float MAX_DELTA = 50;

    /** Tamaño actual de la pantalla en píxeles. */
    public static int WIDTH = WINDOWED_WIDTH;
    public static int HEIGHT = WINDOWED_HEIGHT;
    private static int pixelScale;
    private static float worldZoom;

    static {
        resize(WINDOWED_WIDTH, WINDOWED_HEIGHT);
    }

    private DisplayManager() {
    }

    public static void resize(int width, int height) {
        WIDTH = width;
        HEIGHT = height;
        pixelScale = Math.max(1, Math.round(height * ART_PIXEL / TARGET_VIEW_HEIGHT));
        worldZoom = PIXEL_PERFECT ? ART_PIXEL / pixelScale : SMOOTH_ZOOM;
    }

    /** Píxeles de pantalla por píxel del pixel art con {@link #PIXEL_PERFECT} (3 en la ventana de 900 px de alto). */
    public static int pixelScale() {
        return pixelScale;
    }

    /**
     * Unidades del mundo por píxel de pantalla: 1 = sin zoom; más = la cámara está más lejos y cabe más mapa. No
     * afecta al HUD.
     */
    public static float worldZoom() {
        return worldZoom;
    }

    /** Único sitio que mide tiempo real: el resto del juego usa {@link GameClock}. Una vez por frame. */
    public static void tick() {
        GameClock.tick(Math.min(Gdx.graphics.getDeltaTime() * 1000, MAX_DELTA));
    }
}
