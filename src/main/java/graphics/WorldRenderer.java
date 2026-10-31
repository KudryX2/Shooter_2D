package graphics;

import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import items.DroppedItem;
import items.Gun;
import items.Item;
import items.StructItem;
import player.BuildTool;
import player.Player;
import structs.Struct;
import structs.StructKind;
import world.Chunk;
import world.Container;
import world.WorldManager;

/**
 * Suelos, muros y cajas (con sus sombras), vista previa de construcción y objetos tirados.
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
    static final float CRATE_HEIGHT = 32;
    private static final float WALL_SHADOW_OFFSET = 8;
    private static final float HALF_WALL_SHADOW_OFFSET = 4;

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
                    int hash = tileHash(s.x(), s.y());
                    float tone = 0.94f + ((hash >>> 8) & 0xFF) / 255f * 0.06f;
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
            for (Container container : chunk.containers) {
                drawShadow(container.struct, HALF_WALL_SHADOW_OFFSET);
            }
        }
    }

    /** Sombra de la base, desplazada hacia abajo a la derecha; con el mismo desplazamiento todas encajan entre sí. */
    private static void drawShadows(List<Struct> structs, float offset) {
        for (Struct s : structs) {
            drawShadow(s, offset);
        }
    }

    private static void drawShadow(Struct s, float offset) {
        if (isVisible(s.x(), s.y())) {
            ShapeRenderer shapes = Draw.shapes();
            shapes.setColor(0, 0, 0, SHADOW_ALPHA);
            float w = s.halfWidth() * 2, h = s.halfHeight() * 2;
            shapes.rect(screenX(s.x()) - w / 2 + offset, screenY(s.y()) - h / 2 + offset, w, h);
        }
    }

    static float height(Struct s) {
        return switch (s.kind()) {
            case WALL -> WALL_HEIGHT;
            case CRATE -> CRATE_HEIGHT;
            default -> HALF_WALL_HEIGHT;
        };
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
        if (s.kind() == StructKind.CRATE) {
            return TextureId.CRATE;
        }
        if (s.kind() == StructKind.WALL) {
            return s.vertical() ? TextureId.WALL_V : TextureId.WALL_H;
        }
        return s.vertical() ? TextureId.HALF_WALL_V : TextureId.HALF_WALL_H;
    }

    /** Muro, saco terrero o caja en 3/4, con el tinte actual. */
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
        if (preview.kind() != StructKind.FLOOR) {
            drawWall(preview);
        } else {
            drawFloor(preview);
        }
        Draw.tint(1, 1, 1, 1);
    }

    // ---------------------------------------------------------------------------------------------
    // Objetos tirados
    // ---------------------------------------------------------------------------------------------

    /**
     * Cada objeto a su tamaño real en píxeles del dibujo. Las armas se ven tumbadas, con su sprite de la mano; los
     * objetos para construir, a la mitad (su icono ocupa una casilla entera).
     */
    static void drawDroppedItems() {
        for (Chunk chunk : WorldManager.activeChunks) {
            for (DroppedItem dropped : chunk.droppedItems) {
                if (!isVisible(dropped.x(), dropped.y())) {
                    continue;
                }
                Item item = dropped.item();
                TextureRegion region = TexturesManager.region(item instanceof Gun ? item.heldTexture : item.icon);
                float scale = DisplayManager.ART_PIXEL * (item instanceof StructItem ? 0.5f : 1);
                float width = region.getRegionWidth() * scale, height = region.getRegionHeight() * scale;
                // La esquina, en un borde de píxel del dibujo: centrado sin más, un sprite de ancho o alto impar
                // (como las armas) queda a medio píxel y alguna fila o columna salta de uno a otro al moverse
                float left = snapToArtPixel(dropped.x() - width / 2);
                float top = snapToArtPixel(dropped.y() - height / 2);
                // Un montón se ve como varias copias amontonadas
                StackDrawing.draw(region, screenX(left), screenY(top), width, height, dropped.count(),
                        DisplayManager.ART_PIXEL);
            }
        }
    }

    /** Coordenada del mundo redondeada a un borde de píxel del dibujo. */
    private static float snapToArtPixel(float worldCoordinate) {
        float art = DisplayManager.ART_PIXEL;
        return Math.round(worldCoordinate / art) * art;
    }
}
