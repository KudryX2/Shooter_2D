package game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import graphics.DisplayManager;
import graphics.GraphicsManager;
import graphics.HudRenderer;
import graphics.PauseButton;
import input.Input;
import player.Player;
import player.PlayerController;
import util.GameClock;
import world.WorldManager;

/**
 * La partida: libGDX llama a {@link #create()} al abrir la ventana y a {@link #render()} en cada frame. El
 * arranque (ventana, argumentos) está en {@link DesktopLauncher}.
 */
public final class Game extends ApplicationAdapter {
    private final boolean classicWorld;

    /** @param classicWorld cargar el mapa de pruebas original ({@link DemoWorld}) en lugar de {@link OutpostWorld} */
    public Game(boolean classicWorld) {
        this.classicWorld = classicWorld;
    }

    @Override
    public void create() {
        DisplayManager.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        GraphicsManager.init();
        Input.init();
        Player.create();
        PlayerController.init();
        if (classicWorld) {
            DemoWorld.create();
        } else {
            OutpostWorld.create();
        }
    }

    @Override
    public void resize(int width, int height) {
        GraphicsManager.resize(width, height);
    }

    @Override
    public void render() {
        DisplayManager.tick();
        Input.update();
        PauseButton.update();
        // En pausa el mundo queda congelado, pero se sigue dibujando
        if (!GameClock.isPaused()) {
            PlayerController.update(PauseButton.isHovered());
            WorldManager.update();
        }

        GraphicsManager.render();

        if (!handleDebugKeys()) {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        GraphicsManager.dispose();
        System.out.println("//////////////////////////////////////////////////////////");
        System.out.println(" TE HAS CARGADO " + WorldManager.zombiesKilled + " ZOMBIES WOW SUCH SKILL VERY AIM ");
        System.out.println("//////////////////////////////////////////////////////////");
    }

    /** Teclas de desarrollo. Devuelve {@code false} si hay que salir del juego. */
    private boolean handleDebugKeys() {
        if (Input.wasKeyPressed(Keys.N)) HudRenderer.hudScale--;
        if (Input.wasKeyPressed(Keys.M)) HudRenderer.hudScale++;
        if (Input.wasKeyPressed(Keys.SPACE)) GameClock.togglePause();

        // En pausa no se lanzan oleadas ni se vuelve al inicio
        if (!GameClock.isPaused()) {
            if (Input.wasKeyPressed(Keys.P)) {
                if (classicWorld) {
                    DemoWorld.spawnWave(DisplayManager.WIDTH);
                } else {
                    OutpostWorld.spawnWave();
                }
            }
            if (Input.isKeyDown(Keys.R)) Player.teleportToSpawn();
        }

        return !Input.isKeyDown(Keys.ESCAPE);
    }
}
