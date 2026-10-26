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
 * <p>Argumentos: {@code --windowed} para jugar en ventana en lugar de pantalla completa.
 */
public final class Game {
    private boolean wasP, wasN, wasM;

    public static void main(String[] args) {
        NativeLibraries.configure();
        boolean fullScreen = !List.of(args).contains("--windowed");
        new Game().run(fullScreen);
    }

    private void run(boolean fullScreen) {
        try {
            DisplayManager.init(fullScreen);
            Player.create();
            TexturesManager.init();
            GraphicsManager.init();
            ItemManager.init();
            NpcManager.init();
            DemoWorld.create();

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
        if (p && !wasP) DemoWorld.spawnWave(DisplayManager.WIDTH);
        wasP = p;

        if (Keyboard.isKeyDown(Keyboard.KEY_R)) Player.teleportToSpawn();

        return !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
    }
}
