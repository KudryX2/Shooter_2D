package graphics;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import util.GameClock;

public final class DisplayManager {
    public static final String TITLE = "Too Late To Retrieve 2D";
    public static final int FPS = 120;
    /** Límite del delta (ms) para evitar saltos enormes tras una pausa (carga, ventana arrastrada...). */
    private static final float MAX_DELTA = 50;

    public static int WIDTH = 1200;
    public static int HEIGHT = 800;

    private static long lastTime;

    private DisplayManager() {
    }

    public static void init(boolean fullScreen) {
        try {
            Display.setTitle(TITLE);
            if (fullScreen) {
                Display.setFullscreen(true);
            } else {
                Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
            }
            Display.create();
        } catch (LWJGLException e) {
            throw new IllegalStateException("No se pudo crear la ventana OpenGL", e);
        }
        WIDTH = Display.getWidth();
        HEIGHT = Display.getHeight();

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WIDTH, HEIGHT, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Mouse.setGrabbed(true);
        lastTime = System.nanoTime();
    }

    public static void update() {
        Display.sync(FPS);
        Display.update();
        glClear(GL_COLOR_BUFFER_BIT);

        // Único sitio que mide tiempo real: el resto del juego usa GameClock
        long now = System.nanoTime();
        GameClock.tick(Math.min((now - lastTime) / 1_000_000f, MAX_DELTA));
        lastTime = now;
    }

    public static boolean isCloseRequested() {
        return Display.isCloseRequested();
    }

    public static void destroy() {
        if (Display.isCreated()) {
            Display.destroy();
        }
    }
}
