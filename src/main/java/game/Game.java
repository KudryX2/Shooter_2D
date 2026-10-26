package game;

import java.util.List;

import org.lwjgl.input.Keyboard;

import graphics.DisplayManager;
import graphics.GraphicsManager;
import graphics.TexturesManager;
import items.ItemManager;
import npc.NpcManager;
import player.Player;
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
    private boolean wasP, wasN, wasM;

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
            TexturesManager.init();
            GraphicsManager.init();
            ItemManager.init();
            NpcManager.init();
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
            Player.update();
            WorldManager.update();

            GraphicsManager.render();
            DisplayManager.update();

            if (!handleDebugKeys()) {
                return;
            }
        }
    }

    /** Teclas de desarrollo. Devuelve {@code false} si hay que salir del juego. */
    private boolean handleDebugKeys() {
        boolean n = Keyboard.isKeyDown(Keyboard.KEY_N);
        if (n && !wasN) GraphicsManager.hudScale--;
        wasN = n;

        boolean m = Keyboard.isKeyDown(Keyboard.KEY_M);
        if (m && !wasM) GraphicsManager.hudScale++;
        wasM = m;

        boolean p = Keyboard.isKeyDown(Keyboard.KEY_P);
        if (p && !wasP) {
            if (classicWorld) {
                DemoWorld.spawnWave(DisplayManager.WIDTH);
            } else {
                OutpostWorld.spawnWave();
            }
        }
        wasP = p;

        if (Keyboard.isKeyDown(Keyboard.KEY_R)) Player.teleportToSpawn();

        return !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
    }
}
