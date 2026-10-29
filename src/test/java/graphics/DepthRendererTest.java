package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import assets.TextureId;
import player.Player;
import structs.Struct;
import structs.StructKind;

class DepthRendererTest {
    /** Unidades del mundo por píxel del dibujo (una casilla de 64 son 16 px). */
    private static final float ART_SCALE = DisplayManager.ART_PIXEL;
    private static final float S = Struct.SIZE;
    private static final float PLAYER_HALF = Player.SIZE / 2;

    @Test
    void aWallRisesAboveItsBaseByItsHeight() {
        Struct wall = Struct.northWall(0, 0, StructKind.WALL); // base de y -40 a -24
        assertEquals(-24, WorldRenderer.depth(wall));
        assertEquals(-40 - WorldRenderer.WALL_HEIGHT, WorldRenderer.spriteTop(wall));
    }

    /** Si las medidas no cuadran, el sprite sale estirado y los píxeles del dibujo, desiguales. */
    @Test
    void theImagesMatchTheWallGeometryPixelForPixel() throws Exception {
        checkSize(TextureId.WALL_H, Struct.northWall(0, 0, StructKind.WALL));
        checkSize(TextureId.WALL_V, Struct.westWall(0, 0, StructKind.WALL));
        checkSize(TextureId.HALF_WALL_H, Struct.northWall(0, 0, StructKind.HALF_WALL));
        checkSize(TextureId.HALF_WALL_V, Struct.westWall(0, 0, StructKind.HALF_WALL));
    }

    private static void checkSize(TextureId id, Struct s) throws Exception {
        BufferedImage image = ImageIO.read(new File("assets-raw/textures/pixel/" + id.file + ".png"));
        assertEquals(s.halfWidth() * 2, image.getWidth() * ART_SCALE, id + ": ancho");
        assertEquals(WorldRenderer.depth(s) - WorldRenderer.spriteTop(s), image.getHeight() * ART_SCALE, id + ": alto");
    }

    @Test
    void aWallHidesWhoeverStandsRightBehindIt() {
        Struct wall = Struct.northWall(0, 1, StructKind.WALL); // base en y = 32
        assertEquals(0, DepthRenderer.distanceToSprite(wall, 0, 32 - 8 - PLAYER_HALF), "pegado al muro por el norte");
        assertTrue(DepthRenderer.distanceToSprite(wall, 0, -S * 2) > 0, "lejos al norte: el muro no llega");
        assertTrue(DepthRenderer.distanceToSprite(wall, S * 2, 0) > 0, "a un lado");
    }

    @Test
    void sandbagsAreLowAndOnlyHideWhatIsRightBehind() {
        Struct bags = Struct.northWall(0, 1, StructKind.HALF_WALL);
        assertEquals(0, DepthRenderer.distanceToSprite(bags, 0, 32 - 6 - PLAYER_HALF));
        assertTrue(DepthRenderer.distanceToSprite(bags, 0, 32 - 6 - PLAYER_HALF - 40) > 0);
    }

    /** Así no queda un hueco transparente rodeado de muro opaco: también se aclaran los tramos de al lado. */
    @Test
    void wallsNextToThePlayerFadeTooAndBecomeOpaqueFurtherAway() {
        float y = 32 - 8 - PLAYER_HALF; // pegado por el norte al muro de la casilla 0
        Struct next = Struct.northWall(1, 1, StructKind.WALL);
        Struct far = Struct.northWall(4, 1, StructKind.WALL);
        assertEquals(DepthRenderer.SEE_THROUGH_ALPHA,
                DepthRenderer.fadeAroundPlayer(DepthRenderer.distanceToSprite(next, 0, y)), "el tramo de al lado");
        assertEquals(1, DepthRenderer.fadeAroundPlayer(DepthRenderer.distanceToSprite(far, 0, y)), "4 casillas más allá");

        float halfway = (DepthRenderer.FADE_NEAR + DepthRenderer.FADE_FAR) / 2;
        float alpha = DepthRenderer.fadeAroundPlayer(halfway);
        assertTrue(alpha > DepthRenderer.SEE_THROUGH_ALPHA && alpha < 1, "degradado entre medias");
    }
}
