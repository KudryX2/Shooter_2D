package graphics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import assets.TextureId;

/** Carga las texturas de {@link TextureId} (archivos de {@code res/}) y las sirve listas para dibujar. */
public final class TexturesManager {
    private static final Map<TextureId, TextureRegion> regions = new EnumMap<>(TextureId.class);
    private static final Map<String, Texture> textures = new HashMap<>();

    private TexturesManager() {
    }

    /** Requiere la ventana creada. Cada archivo se carga una sola vez aunque lo usen varias constantes. */
    public static void init() {
        for (TextureId id : TextureId.values()) {
            Texture texture = textures.computeIfAbsent(id.file, TexturesManager::load);
            TextureRegion region = new TextureRegion(texture);
            region.flip(false, true); // la cámara tiene la y hacia abajo
            regions.put(id, region);
        }
    }

    private static Texture load(String file) {
        Texture texture = new Texture(Gdx.files.classpath("res/" + file + ".png"));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    static TextureRegion region(TextureId id) {
        TextureRegion region = regions.get(id);
        if (region == null) {
            throw new IllegalStateException("Textura no cargada (¿falta TexturesManager.init()?): " + id);
        }
        return region;
    }

    public static void dispose() {
        textures.values().forEach(Texture::dispose);
        textures.clear();
        regions.clear();
    }
}
