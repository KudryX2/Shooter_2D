package graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pilas de objetos (en el HUD y en el suelo) y su cantidad (con {@link PixelText}).
 *
 * <p>Una pila se dibuja con su icono repetido y desplazado, como un montón: así se distingue una suelta de muchas
 * antes de leer el número. Todo va por píxeles enteros del dibujo para que salga nítido.
 */
final class StackDrawing {
    /** Hasta cuántos hay en la pila se dibuja una sola copia, dos... (a partir del último, tres). */
    private static final int[] COPIES_FROM = {1, 2, 5};
    /** Píxeles del dibujo entre una copia de la pila y la siguiente (con 1 parecía un borde mal dibujado). */
    private static final int COPY_OFFSET = 2;

    private StackDrawing() {
    }

    /** Copias del icono que se dibujan para una pila de {@code count}: 1, 2 (de 2 a 4) o 3 (5 o más). */
    static int copies(int count) {
        int copies = 1;
        for (int i = 1; i < COPIES_FROM.length; i++) {
            if (count >= COPIES_FROM[i]) {
                copies = i + 1;
            }
        }
        return copies;
    }

    /**
     * Pila de {@code count} centrada donde iría una sola en (x, y): cada copia de detrás va {@link #COPY_OFFSET}
     * píxeles más arriba y a la derecha que la anterior.
     */
    static void draw(TextureRegion icon, float x, float y, float width, float height, int count, float pixel) {
        int copies = copies(count);
        float step = COPY_OFFSET * pixel;
        float shift = (copies - 1) * COPY_OFFSET / 2 * pixel; // centra el montón (en píxeles enteros)
        for (int i = copies - 1; i >= 0; i--) {
            Draw.region(icon, x - shift + i * step, y + shift - i * step, width, height, 0);
        }
    }

    /** {@code number} con su última cifra acabando en (right, bottom), a {@code pixel} unidades por píxel. */
    static void number(int number, float right, float bottom, float pixel) {
        PixelText.drawRightAligned(Integer.toString(number), right, bottom, pixel);
    }
}
