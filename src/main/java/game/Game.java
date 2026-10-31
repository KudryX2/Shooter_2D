package game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import assets.Assets;
import graphics.DisplayManager;
import graphics.GraphicsManager;
import graphics.HudRenderer;
import graphics.InventoryPanel;
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

    /** Mientras se cargan las texturas solo se dibuja la barra de progreso. */
    private boolean loading = true;

    @Override
    public void create() {
        DisplayManager.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        GraphicsManager.init();
        Input.init();
        Assets.startLoading();
    }

    /** Cuando ya está todo cargado: crea la partida. */
    private void start() {
        GraphicsManager.onAssetsLoaded();
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
        if (loading) {
            if (Assets.update()) {
                loading = false;
                start();
            } else {
                GraphicsManager.renderLoading(Assets.progress());
                return;
            }
        }

        DisplayManager.tick();
        Input.update();
        PauseButton.update();
        InventoryPanel.update();
        // En pausa el mundo queda congelado, pero se sigue dibujando
        if (!GameClock.isPaused()) {
            // Con el inventario abierto el ratón es para él: no se dispara ni se construye
            PlayerController.update(PauseButton.isHovered() || InventoryPanel.isOpen());
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
        if (Input.wasKeyPressed(Keys.P) || Input.wasKeyPressed(Keys.SPACE)) GameClock.togglePause();

        // En pausa no se lanzan oleadas ni se vuelve al inicio
        if (!GameClock.isPaused()) {
            if (Input.wasKeyPressed(Keys.H)) { // horda
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
