package graphics;

import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;

import java.util.List;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import items.DroppedItem;
import items.StructItem;
import player.BuildTool;
import player.Player;
import structs.Struct;
import structs.StructKind;
import world.Chunk;
import world.WorldManager;

/**
 * Suelos, muros (con sus sombras), vista previa de construcción y objetos tirados.
 *
 * <p>Vista en 3/4: la base de un muro (su caja de colisión) está en el suelo y el muro "sube" en pantalla
 * {@link #height} unidades, así que se ve su remate arriba y su cara frontal (la que mira al sur) debajo. El orden
 * en que se dibujan muros y personajes lo decide {@link DepthRenderer}.
 */
final class WorldRenderer {
    /** Opacidad de todas las sombras (también las de los personajes). */
    static final float SHADOW_ALPHA = 0.3f;
    /** Altura de la cara frontal; tiene que cuadrar con los PNG de {@code tools.PixelArtGenerator}. */
    static final float WALL_HEIGHT = 48;
    static final float HALF_WALL_HEIGHT = 24;
    private static final float WALL_SHADOW_OFFSET = 8;
    private static final float HALF_WALL_SHADOW_OFFSET = 4;
    private static final float DROPPED_ITEM_SIZE = 32;

    private WorldRenderer() {
    }

    // ---------------------------------------------------------------------------------------------
    // Suelos
    // ---------------------------------------------------------------------------------------------

    /** Cada casilla elige variante y un tono ligeramente distinto según su posición, para que no se note la repetición. */
    static void drawFloors() {
        for (Chunk chunk : WorldManager.activeChunks) {
            for (Struct s : chunk.floors) {
                if (isVisible(s.x(), s.y())) {
                    boolean picture = s.texture() == TextureId.TEEMO; // las imágenes van tal cual
                    int hash = tileHash(s.x(), s.y());
                    float tone = picture ? 1 : 0.94f + ((hash >>> 8) & 0xFF) / 255f * 0.06f;
                    Draw.tint(tone, tone, tone, 1);
                    drawFloor(s);
                }
            }
        }
        Draw.tint(1, 1, 1, 1);
    }

    /** Una casilla de suelo con el tinte actual; la variante sale de su posición (siempre la misma). */
    private static void drawFloor(Struct s) {
        float half = Struct.SIZE / 2;
        int variant = tileHash(s.x(), s.y()) >>> 16;
        Draw.region(TexturesManager.variant(s.texture(), variant), screenX(s.x()) - half, screenY(s.y()) - half,
                Struct.SIZE, Struct.SIZE, 0);
    }

    private static int tileHash(float x, float y) {
        int h = (int) x * 73_856_093 ^ (int) y * 19_349_663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return h ^ (h >>> 15);
    }

    // ---------------------------------------------------------------------------------------------
    // Muros
    // ---------------------------------------------------------------------------------------------

    static void drawWallShadows() {
        for (Chunk chunk : WorldManager.activeChunks) {
            drawShadows(chunk.walls, WALL_SHADOW_OFFSET);
            drawShadows(chunk.halfWalls, HALF_WALL_SHADOW_OFFSET);
        }
    }

    /** Sombra de la base, desplazada hacia abajo a la derecha; con el mismo desplazamiento todas encajan entre sí. */
    private static void drawShadows(List<Struct> structs, float offset) {
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0, 0, 0, SHADOW_ALPHA);
        for (Struct s : structs) {
            if (isVisible(s.x(), s.y())) {
                float w = s.halfWidth() * 2, h = s.halfHeight() * 2;
                shapes.rect(screenX(s.x()) - w / 2 + offset, screenY(s.y()) - h / 2 + offset, w, h);
            }
        }
    }

    static float height(Struct s) {
        return s.kind() == StructKind.WALL ? WALL_HEIGHT : HALF_WALL_HEIGHT;
    }

    /** Hasta dónde llega la base por abajo (el sur): lo que está más al sur se dibuja encima. */
    static float depth(Struct s) {
        return s.y() + s.halfHeight();
    }

    /** Lo alto del sprite (en coordenadas del mundo): desde el remate hasta el pie de la base. */
    static float spriteTop(Struct s) {
        return s.y() - s.halfHeight() - height(s);
    }

    private static TextureId sideTexture(Struct s) {
        if (s.kind() == StructKind.WALL) {
            return s.vertical() ? TextureId.WALL_V : TextureId.WALL_H;
        }
        return s.vertical() ? TextureId.HALF_WALL_V : TextureId.HALF_WALL_H;
    }

    /** Muro o saco terrero en 3/4, con el tinte actual. */
    static void drawWall(Struct s) {
        float w = s.halfWidth() * 2;
        float top = spriteTop(s);
        Draw.sprite(sideTexture(s), screenX(s.x()) - w / 2, screenY(top), w, depth(s) - top);
    }

    /** Vista previa en el mundo de lo que se va a construir: verde si se puede, rojo si no. */
    static void drawBuildPreview() {
        Struct preview = BuildTool.preview();
        if (preview == null || !(Player.selectedItem() instanceof StructItem)) {
            return;
        }
        if (BuildTool.canBuildPreview()) {
            Draw.tint(0.6f, 1f, 0.6f, 0.7f);
        } else {
            Draw.tint(1f, 0.4f, 0.4f, 0.7f);
        }
        if (preview.isWall()) {
            drawWall(preview);
        } else {
            drawFloor(preview);
        }
        Draw.tint(1, 1, 1, 1);
    }

    // ---------------------------------------------------------------------------------------------
    // Objetos tirados
    // ---------------------------------------------------------------------------------------------

    static void drawDroppedItems() {
        float half = DROPPED_ITEM_SIZE / 2;
        for (Chunk chunk : WorldManager.activeChunks) {
            for (DroppedItem dropped : chunk.droppedItems) {
                Draw.sprite(dropped.item().icon, screenX(dropped.x()) - half, screenY(dropped.y()) - half,
                        DROPPED_ITEM_SIZE, DROPPED_ITEM_SIZE);
            }
        }
    }
}
