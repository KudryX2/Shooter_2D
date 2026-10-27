package graphics;

import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;
import static graphics.Sprite.drawSprite;
import static graphics.Sprite.drawSpriteCropped;
import static graphics.TexturesManager.tex;
import static org.lwjgl.opengl.GL11.*;

import java.util.List;

import assets.TextureId;
import items.DroppedItem;
import items.StructItem;
import player.BuildTool;
import player.Player;
import structs.Struct;
import world.Chunk;
import world.WorldManager;

/** Suelos, muros (con sus sombras), vista previa de construcción y objetos tirados. */
final class WorldRenderer {
    /** Opacidad de todas las sombras (también las de los personajes). */
    static final float SHADOW_ALPHA = 0.3f;
    private static final float WALL_SHADOW_OFFSET = 8;
    private static final float HALF_WALL_SHADOW_OFFSET = 4;
    private static final float DROPPED_ITEM_SIZE = 32;

    private WorldRenderer() {
    }

    /** Cada casilla gira 0/90/180/270º y cambia un poco de tono según su posición, para que no se note la repetición. */
    static void drawFloors() {
        float half = Struct.SIZE / 2;
        for (Chunk chunk : WorldManager.activeChunks) {
            for (Struct s : chunk.floors) {
                if (!isVisible(s.x(), s.y())) {
                    continue;
                }
                int hash = tileHash(s.x(), s.y());
                boolean picture = s.texture() == TextureId.TEEMO; // las imágenes no se giran
                float rotation = picture ? 0 : (hash & 3) * 90;
                float tone = picture ? 1 : 0.9f + ((hash >>> 8) & 0xFF) / 255f * 0.1f;
                glColor3f(tone, tone, tone);
                drawSprite(tex(s.texture()),
                        screenX(s.x()) - half, screenY(s.y()) - half, Struct.SIZE, Struct.SIZE, rotation);
            }
        }
        glColor3f(1f, 1f, 1f);
    }

    private static int tileHash(float x, float y) {
        int h = (int) x * 73_856_093 ^ (int) y * 19_349_663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return h ^ (h >>> 15);
    }

    static void drawWallShadows() {
        for (Chunk chunk : WorldManager.activeChunks) {
            drawShadows(chunk.walls, WALL_SHADOW_OFFSET);
            drawShadows(chunk.halfWalls, HALF_WALL_SHADOW_OFFSET);
        }
    }

    /** Sombra proyectada hacia abajo a la derecha; como todas tienen el mismo desplazamiento, encajan entre sí. */
    private static void drawShadows(List<Struct> structs, float offset) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0, 0, 0, SHADOW_ALPHA);
        glBegin(GL_QUADS);
        for (Struct s : structs) {
            if (isVisible(s.x(), s.y())) {
                float w = s.halfWidth() * 2, h = s.halfHeight() * 2;
                float x = screenX(s.x()) - w / 2 + offset;
                float y = screenY(s.y()) - h / 2 + offset;
                glVertex2f(x, y);
                glVertex2f(x + w, y);
                glVertex2f(x + w, y + h);
                glVertex2f(x, y + h);
            }
        }
        glEnd();
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }

    static void drawWalls() {
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.walls);
        // Medios muros tintados de color saco terrero: se distinguen de los muros (las balas los atraviesan)
        glColor3f(0.85f, 0.72f, 0.5f);
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.halfWalls);
        glColor3f(1f, 1f, 1f);
    }

    private static void drawStructs(List<Struct> structs) {
        for (Struct s : structs) {
            if (isVisible(s.x(), s.y())) {
                drawStruct(s);
            }
        }
    }

    /** Un muro fino usa una tira de su textura a escala real, en vez de aplastar la imagen entera. */
    private static void drawStruct(Struct s) {
        float w = s.halfWidth() * 2, h = s.halfHeight() * 2;
        drawSpriteCropped(tex(s.texture()), screenX(s.x()) - w / 2, screenY(s.y()) - h / 2, w, h,
                Math.min(1, w / Struct.SIZE), Math.min(1, h / Struct.SIZE));
    }

    /** Vista previa en el mundo de lo que se va a construir: verde si se puede, rojo si no. */
    static void drawBuildPreview() {
        Struct preview = BuildTool.preview();
        if (preview == null || !(Player.selectedItem() instanceof StructItem)) {
            return;
        }
        if (BuildTool.canBuildPreview()) {
            glColor4f(0.6f, 1f, 0.6f, 0.7f);
        } else {
            glColor4f(1f, 0.4f, 0.4f, 0.7f);
        }
        drawStruct(preview);
        glColor4f(1, 1, 1, 1);
    }

    static void drawDroppedItems() {
        float half = DROPPED_ITEM_SIZE / 2;
        for (Chunk chunk : WorldManager.activeChunks) {
            for (DroppedItem dropped : chunk.droppedItems) {
                drawSprite(tex(dropped.item().icon), screenX(dropped.x()) - half, screenY(dropped.y()) - half,
                        DROPPED_ITEM_SIZE, DROPPED_ITEM_SIZE);
            }
        }
    }
}
