package graphics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.newdawn.slick.opengl.Texture;

import assets.TextureId;

/** Carga las texturas de {@link TextureId} y las sirve por nombre. */
public final class TexturesManager {
    private static final Map<TextureId, Texture> textures = new EnumMap<>(TextureId.class);

    private TexturesManager() {
    }

    /** Requiere un contexto OpenGL ya creado. Cada archivo se carga una sola vez aunque lo usen varias constantes. */
    public static void init() {
        Map<String, Texture> byFile = new HashMap<>();
        for (TextureId id : TextureId.values()) {
            textures.put(id, byFile.computeIfAbsent(id.file, file -> Sprite.loadTexture("res/" + file + ".png")));
        }
    }

    /** Atajo de {@link #get} para el código de dibujo. */
    static Texture tex(TextureId id) {
        return get(id);
    }

    public static Texture get(TextureId id) {
        Texture texture = textures.get(id);
        if (texture == null) {
            throw new IllegalStateException("Textura no cargada (¿falta TexturesManager.init()?): " + id);
        }
        return texture;
    }
}
