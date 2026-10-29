package graphics;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import assets.Assets;
import assets.TextureId;

/**
 * Saca del atlas ({@link Assets#atlas()}) las regiones de cada {@link TextureId} y las sirve listas para dibujar.
 *
 * <p>Si en {@code assets-raw/textures} hay frames numerados ({@code 4-zombie_0.png}, {@code 4-zombie_1.png}...)
 * la textura se anima sola, a {@link #FRAME_MS} por frame. Con una sola imagen se ve siempre igual. Los suelos
 * usan esas imágenes numeradas como variantes ({@link #variant}) en lugar de como animación.
 */
public final class TexturesManager {
    /** Duración de cada frame de las animaciones. */
    static final float FRAME_MS = 100;

    private static final Map<TextureId, Animation<TextureRegion>> animations = new EnumMap<>(TextureId.class);

    private TexturesManager() {
    }

    /** Requiere el atlas cargado. */
    static void init() {
        TextureAtlas atlas = Assets.atlas();
        for (TextureId id : TextureId.values()) {
            Array<AtlasRegion> frames = atlas.findRegions(id.file);
            if (frames.isEmpty()) {
                throw new IllegalStateException("Falta la textura " + id + " en el atlas: " + id.file + ".png");
            }
            frames.sort(Comparator.comparingInt(region -> region.index));
            animations.put(id, new Animation<>(FRAME_MS, frames, Animation.PlayMode.LOOP));
        }
    }

    /** Primer frame (el único si no está animada). */
    static TextureRegion region(TextureId id) {
        return frame(id, 0);
    }

    /** Variante número {@code n} (cualquier entero: da la vuelta) de una textura con imágenes numeradas. */
    static TextureRegion variant(TextureId id, int n) {
        TextureRegion[] variants = animation(id).getKeyFrames();
        return variants[Math.floorMod(n, variants.length)];
    }

    /** Frame que toca a los {@code timeMs} ms de animación. */
    static TextureRegion frame(TextureId id, float timeMs) {
        return animation(id).getKeyFrame(timeMs);
    }

    private static Animation<TextureRegion> animation(TextureId id) {
        Animation<TextureRegion> animation = animations.get(id);
        if (animation == null) {
            throw new IllegalStateException("Textura no cargada (¿falta TexturesManager.init()?): " + id);
        }
        return animation;
    }

    /** Las texturas son del atlas: las libera {@link Assets#dispose()}. */
    static void dispose() {
        animations.clear();
    }
}
