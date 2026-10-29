package assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader;
import com.badlogic.gdx.assets.loaders.resolvers.ClasspathFileHandleResolver;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/**
 * Carga todo lo que el juego lee de disco (de momento, el atlas de texturas) con el {@link AssetManager} de libGDX,
 * poco a poco para poder enseñar una pantalla de carga. Más adelante también sonidos y mapas.
 *
 * <p>Uso: {@link #startLoading()}, luego {@link #update()} cada frame hasta que devuelva {@code true}.
 */
public final class Assets {
    /** Lo genera la tarea {@code packTextures} de Gradle a partir de {@code assets-raw/textures}. */
    public static final String ATLAS = "atlas/game.atlas";
    /** Tiempo máximo de carga por frame, para que la pantalla de carga siga respondiendo. */
    private static final int LOAD_BUDGET_MS = 15;

    private static AssetManager manager;

    private Assets() {
    }

    /** Requiere la ventana creada. */
    public static void startLoading() {
        manager = new AssetManager(new ClasspathFileHandleResolver());
        // La cámara tiene la y hacia abajo: las regiones se cargan ya volteadas
        manager.load(ATLAS, TextureAtlas.class, new TextureAtlasLoader.TextureAtlasParameter(true));
    }

    /** Sigue cargando un rato. Devuelve {@code true} cuando ya está todo. */
    public static boolean update() {
        return manager.update(LOAD_BUDGET_MS);
    }

    /** De 0 a 1. */
    public static float progress() {
        return manager.getProgress();
    }

    public static TextureAtlas atlas() {
        return manager.get(ATLAS, TextureAtlas.class);
    }

    public static void dispose() {
        if (manager != null) {
            manager.dispose();
            manager = null;
        }
    }
}
