package graphics;

import com.badlogic.gdx.Gdx;

import util.GameClock;

/** Tamaño de la pantalla y paso del tiempo real al reloj del juego. */
public final class DisplayManager {
    public static final String TITLE = "Too Late To Retrieve 2D";
    public static final int FPS = 120;
    /** Tamaño de la ventana (si la pantalla es más pequeña, se reduce para que quepa). */
    public static final int WINDOWED_WIDTH = 1600;
    public static final int WINDOWED_HEIGHT = 900;
    /**
     * Cuánto se aleja la cámara del mundo: 1 = un píxel de pantalla por unidad del mundo; 1.25 = todo se ve al
     * 80 % y cabe más mapa. No afecta al HUD.
     */
    public static final float WORLD_ZOOM = 1.25f;
    /** Límite del delta (ms) para evitar saltos enormes tras una pausa (carga, ventana arrastrada...). */
    private static final float MAX_DELTA = 50;

    /** Tamaño actual de la pantalla en píxeles. */
    public static int WIDTH = WINDOWED_WIDTH;
    public static int HEIGHT = WINDOWED_HEIGHT;

    private DisplayManager() {
    }

    public static void resize(int width, int height) {
        WIDTH = width;
        HEIGHT = height;
    }

    /** Único sitio que mide tiempo real: el resto del juego usa {@link GameClock}. Una vez por frame. */
    public static void tick() {
        GameClock.tick(Math.min(Gdx.graphics.getDeltaTime() * 1000, MAX_DELTA));
    }
}
