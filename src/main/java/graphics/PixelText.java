package graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import assets.TextureId;

/**
 * Texto con los signos en pixel art que hay: cifras ({@link TextureId#DIGITS}), {@code /}, {@code (}, {@code )} y
 * espacios. Basta para cantidades como {@code 7/12 (48)}; para texto de verdad hará falta una fuente.
 *
 * <p>Cada signo lleva un contorno oscuro de 1 px, y dos signos seguidos lo comparten.
 */
final class PixelText {
    /** Ancho de un espacio, en píxeles del dibujo. */
    private static final int SPACE = 2;

    private PixelText() {
    }

    /** Ancho de {@code text}, en píxeles del dibujo. */
    static int width(String text) {
        int width = 1; // el contorno de la derecha del último signo
        for (int i = 0; i < text.length(); i++) {
            width += advance(text.charAt(i));
        }
        return width;
    }

    /** {@code text} acabando en (right, bottom), a {@code pixel} unidades por píxel del dibujo. */
    static void drawRightAligned(String text, float right, float bottom, float pixel) {
        draw(text, right - width(text) * pixel, bottom, pixel);
    }

    /** {@code text} empezando en (left, bottom), a {@code pixel} unidades por píxel del dibujo. */
    static void draw(String text, float left, float bottom, float pixel) {
        float x = left;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            TextureRegion glyph = glyph(c);
            if (glyph != null) {
                Draw.region(glyph, x, bottom - glyph.getRegionHeight() * pixel, glyph.getRegionWidth() * pixel,
                        glyph.getRegionHeight() * pixel, 0);
            }
            x += advance(c) * pixel;
        }
    }

    /** De un signo al siguiente: su ancho sin el contorno de la derecha (que se comparte). */
    private static int advance(char c) {
        TextureRegion glyph = glyph(c);
        return glyph == null ? SPACE : glyph.getRegionWidth() - 1;
    }

    /** La imagen de un signo, o {@code null} para un espacio. */
    private static TextureRegion glyph(char c) {
        return switch (c) {
            case ' ' -> null;
            case '/' -> TexturesManager.region(TextureId.SLASH);
            case '(' -> TexturesManager.region(TextureId.PAREN_OPEN);
            case ')' -> TexturesManager.region(TextureId.PAREN_CLOSE);
            default -> {
                if (c < '0' || c > '9') {
                    throw new IllegalArgumentException("Sin dibujo para '" + c + "'");
                }
                yield TexturesManager.variant(TextureId.DIGITS, c - '0');
            }
        };
    }
}
