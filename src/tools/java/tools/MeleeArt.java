package tools;

import java.awt.image.BufferedImage;

/**
 * Armas cuerpo a cuerpo provisionales en pixel art para {@link PixelArtGenerator}, con la punta (o la hoja) a la
 * derecha. Igual que en {@link GunArt}, cada una tiene su icono ({@code 5-nombre}, 18x18) y su imagen en la mano
 * ({@code 6-nombre}), con el punto por donde se agarra en el píxel central.
 */
final class MeleeArt {
    private static final int METAL_LIGHT = 0xc4c9cf, METAL = 0x8a9099, METAL_DARK = 0x4f535a;
    private static final int GRIP = 0x2e2a27;
    private static final int WOOD_LIGHT = 0xb5824e, WOOD = 0x8f5d31, WOOD_DARK = 0x5e3a1d;

    /** Medidas del arma y dónde la agarra la mano (relativo a su esquina superior izquierda). */
    enum Melee {
        KNIFE("knife", 9, 3, 1, 1),
        MACHETE("machete", 13, 3, 1, 1),
        BAT("bat", 14, 3, 2, 1),
        AXE("axe", 13, 5, 1, 2),
        SHOVEL("shovel", 16, 5, 2, 2);

        final String file;
        final int width, height, gripX, gripY;

        Melee(String file, int width, int height, int gripX, int gripY) {
            this.file = file;
            this.width = width;
            this.height = height;
            this.gripX = gripX;
            this.gripY = gripY;
        }
    }

    private MeleeArt() {
    }

    static BufferedImage icon(Melee melee) {
        Canvas c = new Canvas(GunArt.ICON_SIZE, GunArt.ICON_SIZE);
        draw(c, melee, (GunArt.ICON_SIZE - melee.width) / 2, (GunArt.ICON_SIZE - melee.height) / 2);
        return c.outlined();
    }

    /** Lo bastante grande para que el agarre quede en el píxel central (y 1 px de margen para el contorno). */
    static BufferedImage held(Melee melee) {
        int centerX = Math.max(melee.gripX, melee.width - 1 - melee.gripX) + 1;
        int centerY = Math.max(melee.gripY, melee.height - 1 - melee.gripY) + 1;
        Canvas c = new Canvas(centerX * 2 + 1, centerY * 2 + 1);
        draw(c, melee, centerX - melee.gripX, centerY - melee.gripY);
        return c.outlined();
    }

    private static void draw(Canvas c, Melee melee, int x, int y) {
        switch (melee) {
            case KNIFE -> knife(c, x, y);
            case MACHETE -> machete(c, x, y);
            case BAT -> bat(c, x, y);
            case AXE -> axe(c, x, y);
            case SHOVEL -> shovel(c, x, y);
        }
    }

    private static void knife(Canvas c, int x, int y) {
        c.rect(x, y + 1, 3, 1, GRIP);              // mango
        c.rect(x + 3, y, 1, 3, METAL_DARK);        // guarda
        c.rect(x + 4, y + 1, 5, 1, METAL_LIGHT);   // hoja
        c.rect(x + 4, y + 2, 3, 1, METAL);
    }

    private static void machete(Canvas c, int x, int y) {
        c.rect(x, y + 1, 3, 1, GRIP);              // mango
        c.rect(x + 3, y, 1, 3, METAL_DARK);        // guarda
        c.rect(x + 4, y + 1, 8, 1, METAL_LIGHT);   // hoja, más ancha hacia la punta
        c.rect(x + 4, y + 2, 9, 1, METAL);
        c.rect(x + 8, y, 4, 1, METAL_LIGHT);
    }

    private static void bat(Canvas c, int x, int y) {
        c.rect(x, y, 1, 3, WOOD_DARK);             // pomo
        c.rect(x + 1, y + 1, 4, 1, WOOD_DARK);     // empuñadura
        c.rect(x + 5, y + 1, 3, 1, WOOD);
        c.rect(x + 8, y, 6, 3, WOOD);              // cuerpo
        c.rect(x + 8, y, 6, 1, WOOD_LIGHT);
    }

    private static void axe(Canvas c, int x, int y) {
        c.rect(x, y + 2, 12, 1, WOOD);             // mango
        c.set(x, y + 2, WOOD_DARK);
        c.rect(x + 10, y + 1, 2, 2, METAL_DARK);   // cabeza
        c.rect(x + 9, y + 3, 4, 2, METAL);         // hoja, hacia abajo
        c.rect(x + 9, y + 4, 4, 1, METAL_LIGHT);
    }

    private static void shovel(Canvas c, int x, int y) {
        c.rect(x, y + 1, 1, 3, WOOD_DARK);         // empuñadura en T
        c.rect(x + 1, y + 2, 11, 1, WOOD);         // mango
        c.set(x + 12, y + 2, METAL_DARK);          // casquillo
        c.rect(x + 13, y, 2, 5, METAL);            // pala, acabada en punta
        c.rect(x + 13, y, 2, 1, METAL_LIGHT);
        c.rect(x + 15, y + 1, 1, 3, METAL);
    }
}
