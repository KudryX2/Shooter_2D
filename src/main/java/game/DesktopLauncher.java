package game;

import java.util.List;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import graphics.DisplayManager;

/**
 * Punto de entrada: crea la ventana y arranca la partida.
 *
 * <p>Argumentos:
 * <ul>
 *   <li>{@code --windowed}: jugar en ventana en lugar de pantalla completa.</li>
 *   <li>{@code --classic}: cargar el mapa de pruebas original ({@link DemoWorld}) en lugar de
 *       {@link OutpostWorld}.</li>
 * </ul>
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        List<String> options = List.of(args);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(DisplayManager.TITLE);
        config.useVsync(false);
        config.setForegroundFPS(DisplayManager.FPS);
        if (options.contains("--windowed")) {
            config.setWindowedMode(DisplayManager.WINDOWED_WIDTH, DisplayManager.WINDOWED_HEIGHT);
            config.setResizable(false);
        } else {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        }

        new Lwjgl3Application(new Game(options.contains("--classic")), config);
    }
}
