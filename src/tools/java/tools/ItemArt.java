package tools;

import java.awt.image.BufferedImage;
import java.util.Random;

/** Bala, sangre, botiquín y HUD (barras de vida y casillas del inventario) en pixel art, para {@link PixelArtGenerator}. */
final class ItemArt {
    static final int CELL_SIZE = 16;

    private ItemArt() {
    }

    /** Bala: 2x2, un núcleo casi blanco y una esquina anaranjada. */
    static BufferedImage bullet() {
        Canvas c = new Canvas(2, 2);
        c.rect(0, 0, 2, 2, 0xfff2b0);
        c.set(1, 1, 0xffc455);
        return c.image();
    }

    /**
     * Relleno de una barra de vida: 1x2, se estira a lo ancho. Arriba un poco más claro (brillo) y abajo el color.
     */
    static BufferedImage healthFill(int light, int base) {
        Canvas c = new Canvas(1, 2);
        c.set(0, 0, light);
        c.set(0, 1, base);
        return c.image();
    }

    /** Casilla del inventario: borde oscuro, bisel claro por dentro y fondo oscuro semitransparente. */
    static BufferedImage cell() {
        Canvas c = new Canvas(CELL_SIZE, CELL_SIZE);
        int last = CELL_SIZE - 1;
        for (int x = 1; x < last; x++) {
            for (int y = 1; y < last; y++) {
                c.setArgb(x, y, 0x5a000000 | 0x1c1a18); // fondo: deja ver un poco el mundo
            }
        }
        frame(c, 0, 0x262220);
        // Bisel: claro arriba y a la izquierda, oscuro abajo y a la derecha
        c.rect(1, 1, CELL_SIZE - 2, 1, 0x8a8478);
        c.rect(1, 1, 1, CELL_SIZE - 2, 0x8a8478);
        c.rect(1, last - 1, CELL_SIZE - 2, 1, 0x4a4640);
        c.rect(last - 1, 1, 1, CELL_SIZE - 2, 0x4a4640);
        roundCorners(c);
        return c.image();
    }

    /** Se dibuja encima de la casilla elegida: marco dorado de 2 px, con el centro transparente. */
    static BufferedImage selectedCell() {
        Canvas c = new Canvas(CELL_SIZE, CELL_SIZE);
        frame(c, 0, 0xffd34d);
        frame(c, 1, 0xd9a520);
        roundCorners(c);
        return c.image();
    }

    private static void frame(Canvas c, int inset, int rgb) {
        int size = CELL_SIZE - inset * 2;
        c.rect(inset, inset, size, 1, rgb);
        c.rect(inset, inset + size - 1, size, 1, rgb);
        c.rect(inset, inset, 1, size, rgb);
        c.rect(inset + size - 1, inset, 1, size, rgb);
    }

    private static void roundCorners(Canvas c) {
        int last = CELL_SIZE - 1;
        c.setArgb(0, 0, 0);
        c.setArgb(last, 0, 0);
        c.setArgb(0, last, 0);
        c.setArgb(last, last, 0);
    }

    /** Charco de sangre de 16x16: mancha irregular con brillos y algunas gotas alrededor. */
    static BufferedImage blood() {
        Canvas c = new Canvas(16, 16);
        Random random = new Random(400);
        int dark = 0x6e0e0e, base = 0x9a1616, light = 0xc23232;
        // Mancha: suma de círculos alrededor del centro
        int[][] blobs = {{8, 8, 5}, {5, 7, 3}, {11, 9, 3}, {8, 11, 3}, {9, 5, 3}};
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int[] b : blobs) {
                    int dx = x - b[0], dy = y - b[1];
                    if (dx * dx + dy * dy <= b[2] * b[2]) {
                        c.set(x, y, random.nextInt(5) == 0 ? dark : base);
                        break;
                    }
                }
            }
        }
        // Brillos y gotas sueltas
        c.set(7, 6, light);
        c.set(8, 6, light);
        c.set(6, 9, light);
        int[][] drops = {{2, 3}, {13, 4}, {14, 12}, {3, 13}, {12, 1}};
        for (int[] d : drops) {
            c.set(d[0], d[1], base);
        }
        c.set(13, 5, dark);
        return c.image();
    }

    /** Botiquín 8x8 (10x10 con el contorno): caja blanca con asa, cruz roja y la base en sombra. */
    static BufferedImage medkit() {
        Canvas c = new Canvas(10, 10);
        c.rect(4, 1, 2, 1, 0x9a9a9a);             // asa
        c.rect(1, 2, 8, 7, 0xf0ece2);             // caja
        c.rect(1, 7, 8, 1, 0xc8c2b4);             // sombra de abajo
        c.rect(1, 8, 8, 1, 0xa8a294);
        c.rect(4, 3, 2, 4, 0xd42c2c);             // cruz
        c.rect(3, 4, 4, 2, 0xd42c2c);
        c.set(4, 3, 0xe85050);                    // brillo de la cruz
        return c.outlined();
    }

    /** Cifras de 3x5 píxeles, fila a fila de arriba abajo ('#' = pintado). */
    private static final String[] DIGITS = {
        "####.##.##.####", // 0
        ".#.##..#..#.###", // 1
        "###..#####..###", // 2
        "###..####..####", // 3
        "#.##.####..#..#", // 4
        "####..###..####", // 5
        "####..####.####", // 6
        "###..#..#..#..#", // 7
        "####.#####.####", // 8
        "####.####..####", // 9
    };

    /**
     * Cifra {@code n} para contar objetos en el HUD (p.ej. una pila del inventario): 3x5 px en blanco con contorno
     * oscuro, así que la imagen es de 5x7.
     */
    static BufferedImage digit(int n) {
        Canvas c = new Canvas(5, 7);
        String pattern = DIGITS[n];
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '#') {
                c.set(1 + i % 3, 1 + i / 3, 0xf4f4ee);
            }
        }
        return c.outlined();
    }
}
