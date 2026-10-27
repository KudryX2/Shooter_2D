package game;

import java.util.List;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;

import graphics.DisplayManager;
import input.Input;

/**
 * Punto de entrada: crea la ventana y arranca la partida.
 *
 * <p>Por ahora el juego siempre se abre en ventana. Argumentos:
 * <ul>
 *   <li>{@code --classic}: cargar el mapa de pruebas original ({@link DemoWorld}) en lugar de
 *       {@link OutpostWorld}.</li>
 * </ul>
 */
public final class DesktopLauncher {
    /** La ventana no ocupa más de esta fracción de la pantalla. */
    private static final float MAX_SCREEN_FRACTION = 0.9f;

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        List<String> options = List.of(args);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(DisplayManager.TITLE);
        config.useVsync(false);
        config.setForegroundFPS(DisplayManager.FPS);
        // Por ahora siempre en ventana; la pantalla completa se añadirá más adelante.
        // En pantallas pequeñas la ventana se reduce (con las mismas proporciones) para que quepa.
        Graphics.DisplayMode screen = Lwjgl3ApplicationConfiguration.getDisplayMode();
        float fit = Math.min(1, MAX_SCREEN_FRACTION * Math.min(
                (float) screen.width / DisplayManager.WINDOWED_WIDTH,
                (float) screen.height / DisplayManager.WINDOWED_HEIGHT));
        config.setWindowedMode(Math.round(DisplayManager.WINDOWED_WIDTH * fit),
                Math.round(DisplayManager.WINDOWED_HEIGHT * fit));
        config.setResizable(false);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void created(Lwjgl3Window window) {
                // Windows a veces abre la ventana detrás de las demás (p.ej. al lanzarla desde el IDE)
                window.focusWindow();
                Input.setFocused(window.isFocused());
            }

            @Override
            public void focusGained() {
                Input.setFocused(true);
            }

            @Override
            public void focusLost() {
                Input.setFocused(false);
            }
        });

        new Lwjgl3Application(new Game(options.contains("--classic")), config);
    }
}
