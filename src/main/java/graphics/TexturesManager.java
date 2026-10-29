package graphics;

import java.util.Comparator;
import java.util.HashMap;
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
 * <p>Si en {@code assets-raw/textures} hay frames numerados ({@code nombre_0.png}, {@code nombre_1.png}...) la
 * textura se anima sola, a {@link #FRAME_MS} por frame. Con una sola imagen se ve siempre igual. Los suelos usan esas
 * imágenes numeradas como variantes ({@link #variant}) en lugar de como animación, y los personajes tienen una
 * animación por pose y dirección ({@link #character}).
 */
public final class TexturesManager {
    /** Duración de cada frame de las animaciones. */
    static final float FRAME_MS = 100;
    /** Al andar, un personaje pasa al siguiente frame cada vez que recorre esta distancia. */
    static final float STRIDE = 32;

    /** Por nombre en el atlas. */
    private static final Map<String, Animation<TextureRegion>> animations = new HashMap<>();

    private TexturesManager() {
    }

    /** Requiere el atlas cargado. */
    static void init() {
        TextureAtlas atlas = Assets.atlas();
        for (TextureId id : TextureId.values()) {
            if (!id.character) {
                load(atlas, id.file, true);
                continue;
            }
            for (String direction : TextureId.DIRECTIONS) {
                load(atlas, id.pose("idle", direction), true);
                load(atlas, id.pose("walk", direction), true);
            }
            load(atlas, id.lying(), false);
        }
    }

    private static void load(TextureAtlas atlas, String name, boolean required) {
        Array<AtlasRegion> frames = atlas.findRegions(name);
        if (frames.isEmpty()) {
            if (required) {
                throw new IllegalStateException("Falta la textura " + name + ".png en el atlas");
            }
            return;
        }
        frames.sort(Comparator.comparingInt(region -> region.index));
        animations.put(name, new Animation<>(FRAME_MS, frames, Animation.PlayMode.LOOP));
    }

    /** Primer frame (el único si no está animada). En los personajes, quieto y de frente. */
    static TextureRegion region(TextureId id) {
        String name = id.character ? id.pose("idle", TextureId.DIRECTIONS[0]) : id.file;
        return animation(name).getKeyFrame(0);
    }

    /** Variante número {@code n} (cualquier entero: da la vuelta) de una textura con imágenes numeradas. */
    static TextureRegion variant(TextureId id, int n) {
        TextureRegion[] variants = animation(id.file).getKeyFrames();
        return variants[Math.floorMod(n, variants.length)];
    }

    /**
     * Un personaje mirando hacia {@code facing}: quieto, o andando con el frame que toca tras recorrer
     * {@code distanceWalked} (así la animación va al ritmo del paso, ande deprisa o despacio).
     */
    static TextureRegion character(TextureId id, Facing facing, boolean walking, float distanceWalked) {
        String direction = TextureId.DIRECTIONS[facing.ordinal()];
        if (!walking) {
            return animation(id.pose("idle", direction)).getKeyFrame(0);
        }
        return animation(id.pose("walk", direction)).getKeyFrame(distanceWalked / STRIDE * FRAME_MS);
    }

    /** El personaje tumbado, o {@code null} si no tiene esa imagen. */
    static TextureRegion lying(TextureId id) {
        Animation<TextureRegion> animation = animations.get(id.lying());
        return animation == null ? null : animation.getKeyFrame(0);
    }

    private static Animation<TextureRegion> animation(String name) {
        Animation<TextureRegion> animation = animations.get(name);
        if (animation == null) {
            throw new IllegalStateException("Textura no cargada (¿falta TexturesManager.init()?): " + name);
        }
        return animation;
    }

    /** Las texturas son del atlas: las libera {@link Assets#dispose()}. */
    static void dispose() {
        animations.clear();
    }
}
