package game;

import java.util.List;

import org.lwjgl.input.Keyboard;

import graphics.DisplayManager;
import graphics.GraphicsManager;
import graphics.PauseButton;
import graphics.TexturesManager;
import input.Input;
import player.Player;
import player.PlayerController;
import util.GameClock;
import world.WorldManager;

/**
 * Punto de entrada.
 *
 * <p>Argumentos:
 * <ul>
 *   <li>{@code --windowed}: jugar en ventana en lugar de pantalla completa.</li>
 *   <li>{@code --classic}: cargar el mapa de pruebas original ({@link DemoWorld}) en lugar de
 *       {@link OutpostWorld}.</li>
 * </ul>
 */
public final class Game {
    private final boolean classicWorld;

    private Game(boolean classicWorld) {
        this.classicWorld = classicWorld;
    }

    public static void main(String[] args) {
        NativeLibraries.configure();
        List<String> options = List.of(args);
        boolean fullScreen = !options.contains("--windowed");
        new Game(options.contains("--classic")).run(fullScreen);
    }

    private void run(boolean fullScreen) {
        try {
            DisplayManager.init(fullScreen);
            Player.create();
            PlayerController.init();
            TexturesManager.init();
            GraphicsManager.init();
            if (classicWorld) {
                DemoWorld.create();
            } else {
                OutpostWorld.create();
            }

            loop();
        } finally {
            DisplayManager.destroy();
        }

        System.out.println("//////////////////////////////////////////////////////////");
        System.out.println(" TE HAS CARGADO " + WorldManager.zombiesKilled + " ZOMBIES WOW SUCH SKILL VERY AIM ");
        System.out.println("//////////////////////////////////////////////////////////");
    }

    private void loop() {
        while (!DisplayManager.isCloseRequested()) {
            Input.update();
            PauseButton.update();
            // En pausa el mundo queda congelado, pero se sigue dibujando
            if (!GameClock.isPaused()) {
                PlayerController.update(PauseButton.isHovered());
                WorldManager.update();
            }

            GraphicsManager.render();
            DisplayManager.update();

            if (!handleDebugKeys()) {
                return;
            }
        }
    }

    /** Teclas de desarrollo. Devuelve {@code false} si hay que salir del juego. */
    private boolean handleDebugKeys() {
        if (Input.wasKeyPressed(Keyboard.KEY_N)) GraphicsManager.hudScale--;
        if (Input.wasKeyPressed(Keyboard.KEY_M)) GraphicsManager.hudScale++;
        if (Input.wasKeyPressed(Keyboard.KEY_SPACE)) GameClock.togglePause();

        // En pausa no se lanzan oleadas ni se vuelve al inicio
        if (!GameClock.isPaused()) {
            if (Input.wasKeyPressed(Keyboard.KEY_P)) {
                if (classicWorld) {
                    DemoWorld.spawnWave(DisplayManager.WIDTH);
                } else {
                    OutpostWorld.spawnWave();
                }
            }
            if (Input.isKeyDown(Keyboard.KEY_R)) Player.teleportToSpawn();
        }

        return !Input.isKeyDown(Keyboard.KEY_ESCAPE);
    }
}
