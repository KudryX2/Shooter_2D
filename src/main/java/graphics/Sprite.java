package graphics;

import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

public final class Sprite {

    private Sprite() {
    }

    public static void drawSprite(Texture texture, float x, float y, float width, float height) {
        drawSprite(texture, x, y, width, height, 0);
    }

    public static void drawSprite(Texture texture, float x, float y, float width, float height, float rotation) {
        texture.bind();

        glTranslatef(x + width / 2, y + height / 2, 0);
        if (rotation != 0) {
            glRotatef(rotation, 0, 0, 1);
        }
        glTranslatef(-width / 2, -height / 2, 0);

        // slick-util amplía las imágenes a potencias de 2 con relleno transparente (p.ej. 20x20 -> 32x32);
        // getWidth()/getHeight() dan la fracción de la textura que ocupa la imagen real
        float u = texture.getWidth();
        float v = texture.getHeight();

        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(0, 0);
        glTexCoord2f(u, 0);
        glVertex2f(width, 0);
        glTexCoord2f(u, v);
        glVertex2f(width, height);
        glTexCoord2f(0, v);
        glVertex2f(0, height);
        glEnd();

        glLoadIdentity();
    }

    /** Carga una textura del classpath (p.ej. {@code "res/1-player.png"}). */
    public static Texture loadTexture(String path) {
        String format = path.substring(path.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        try (InputStream in = Sprite.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró la textura: " + path);
            }
            return TextureLoader.getTexture(format, in);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la textura: " + path, e);
        }
    }
}
