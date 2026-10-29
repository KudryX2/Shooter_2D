package tools;

import java.awt.image.BufferedImage;

/** Lienzo de píxeles para dibujar sprites a mano: 0 = transparente, el resto colores RGB opacos. */
final class Canvas {
    private static final int OUTLINE = 0x231a16;

    private final int[][] pixels;
    final int width, height;

    Canvas(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width][height];
    }

    /** Lo que se salga del lienzo se ignora. */
    void set(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            pixels[x][y] = 0xFF00_0000 | rgb;
        }
    }

    /** Píxel con transparencia ({@code argb}: el byte alto es la opacidad). */
    void setArgb(int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            pixels[x][y] = argb;
        }
    }

    void rect(int x0, int y0, int w, int h, int rgb) {
        for (int x = x0; x < x0 + w; x++) {
            for (int y = y0; y < y0 + h; y++) {
                set(x, y, rgb);
            }
        }
    }

    /** Oscurece la columna derecha de lo pintado dentro del rectángulo (luz desde la izquierda). */
    void shadeRight(int x0, int y0, int w, int h) {
        int x = x0 + w - 1;
        for (int y = y0; y < y0 + h; y++) {
            if (x < width && y < height && pixels[x][y] != 0) {
                pixels[x][y] = 0xFF00_0000 | darker(pixels[x][y], 0.2f);
            }
        }
    }

    /** Da la vuelta en horizontal. */
    void mirror() {
        for (int x = 0; x < width / 2; x++) {
            int[] column = pixels[x];
            pixels[x] = pixels[width - 1 - x];
            pixels[width - 1 - x] = column;
        }
    }

    /** Copia girada 90º en el sentido de las agujas del reloj (lo de arriba queda a la derecha). */
    Canvas rotated() {
        Canvas rotated = new Canvas(height, width);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                rotated.pixels[height - 1 - y][x] = pixels[x][y];
            }
        }
        return rotated;
    }

    /** La imagen tal cual, sin contorno. */
    BufferedImage image() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, pixels[x][y]);
            }
        }
        return image;
    }

    /** La imagen, con un contorno oscuro de 1 px alrededor de lo pintado. */
    BufferedImage outlined() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pixels[x][y] != 0) {
                    image.setRGB(x, y, pixels[x][y]);
                } else if (filled(x - 1, y) || filled(x + 1, y) || filled(x, y - 1) || filled(x, y + 1)) {
                    image.setRGB(x, y, 0xFF00_0000 | OUTLINE);
                }
            }
        }
        return image;
    }

    private boolean filled(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height && pixels[x][y] != 0;
    }

    static int darker(int rgb, float amount) {
        int r = Math.round(((rgb >> 16) & 0xFF) * (1 - amount));
        int g = Math.round(((rgb >> 8) & 0xFF) * (1 - amount));
        int b = Math.round((rgb & 0xFF) * (1 - amount));
        return (r << 16) | (g << 8) | b;
    }
}
