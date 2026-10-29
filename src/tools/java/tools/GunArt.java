package tools;

import java.awt.image.BufferedImage;

/**
 * Armas provisionales en pixel art para {@link PixelArtGenerator}, apuntando a la derecha. Cada una tiene:
 * <ul>
 *   <li>Icono ({@code 5-nombre}, 18x18): el arma centrada, para el inventario y el suelo.</li>
 *   <li>En la mano ({@code 6-nombre}): la empuñadura queda en el píxel central, porque el juego la gira alrededor
 *       del centro de la imagen (la mano). Se dibuja a 4 unidades del mundo por píxel, como los personajes.</li>
 * </ul>
 */
final class GunArt {
    static final int ICON_SIZE = 18;

    private static final int METAL_LIGHT = 0x9aa0a8, METAL = 0x666b73, METAL_DARK = 0x3c3f45;
    private static final int GRIP = 0x2e2a27;
    private static final int WOOD_LIGHT = 0xa8713f, WOOD = 0x86552c, WOOD_DARK = 0x5e3a1d;
    private static final int LENS = 0x74c0e0;

    /** Medidas del arma y dónde la agarra la mano (relativo a su esquina superior izquierda). */
    enum Gun {
        PISTOL("pistol", 8, 5, 2, 3),
        SHOTGUN("shootgun", 15, 4, 4, 2),
        SNIPER("sniper", 17, 5, 5, 3),
        AK47("ak47", 15, 6, 5, 3);

        final String file;
        final int width, height, gripX, gripY;

        Gun(String file, int width, int height, int gripX, int gripY) {
            this.file = file;
            this.width = width;
            this.height = height;
            this.gripX = gripX;
            this.gripY = gripY;
        }
    }

    private GunArt() {
    }

    static BufferedImage icon(Gun gun) {
        Canvas c = new Canvas(ICON_SIZE, ICON_SIZE);
        draw(c, gun, (ICON_SIZE - gun.width) / 2, (ICON_SIZE - gun.height) / 2);
        return c.outlined();
    }

    /** Lo bastante grande para que la empuñadura quede en el píxel central (y 1 px de margen para el contorno). */
    static BufferedImage held(Gun gun) {
        int centerX = Math.max(gun.gripX, gun.width - 1 - gun.gripX) + 1;
        int centerY = Math.max(gun.gripY, gun.height - 1 - gun.gripY) + 1;
        Canvas c = new Canvas(centerX * 2 + 1, centerY * 2 + 1);
        draw(c, gun, centerX - gun.gripX, centerY - gun.gripY);
        return c.outlined();
    }

    private static void draw(Canvas c, Gun gun, int x, int y) {
        switch (gun) {
            case PISTOL -> pistol(c, x, y);
            case SHOTGUN -> shotgun(c, x, y);
            case SNIPER -> sniper(c, x, y);
            case AK47 -> ak47(c, x, y);
        }
    }

    private static void pistol(Canvas c, int x, int y) {
        c.rect(x, y, 8, 2, METAL);              // corredera
        c.rect(x + 1, y, 6, 1, METAL_LIGHT);
        c.set(x + 7, y + 1, METAL_DARK);         // boca
        c.rect(x + 3, y + 2, 2, 1, METAL_DARK);  // guardamonte
        c.rect(x + 1, y + 2, 2, 3, GRIP);        // empuñadura
    }

    private static void shotgun(Canvas c, int x, int y) {
        c.rect(x, y + 1, 3, 3, WOOD);            // culata
        c.rect(x, y + 1, 3, 1, WOOD_LIGHT);
        c.rect(x + 3, y, 4, 3, METAL_DARK);      // cajón
        c.rect(x + 3, y, 4, 1, METAL);
        c.rect(x + 7, y, 8, 1, METAL);           // cañón
        c.rect(x + 7, y + 1, 8, 1, METAL_DARK);  // depósito
        c.rect(x + 9, y + 2, 4, 1, WOOD);        // corredera (pump)
        c.set(x + 4, y + 3, WOOD_DARK);          // empuñadura
    }

    private static void sniper(Canvas c, int x, int y) {
        c.rect(x + 5, y, 5, 1, METAL_DARK);      // mira telescópica
        c.set(x + 9, y, LENS);
        c.set(x + 7, y + 1, METAL_DARK);
        c.rect(x, y + 2, 4, 2, WOOD);            // culata
        c.rect(x, y + 2, 4, 1, WOOD_LIGHT);
        c.rect(x + 1, y + 4, 2, 1, WOOD_DARK);
        c.rect(x + 4, y + 2, 6, 2, METAL);       // cajón
        c.rect(x + 4, y + 2, 6, 1, METAL_LIGHT);
        c.rect(x + 10, y + 2, 7, 1, METAL_DARK); // cañón largo
        c.set(x + 5, y + 4, WOOD_DARK);          // empuñadura
    }

    private static void ak47(Canvas c, int x, int y) {
        c.rect(x, y + 1, 4, 2, WOOD);            // culata
        c.rect(x, y + 1, 4, 1, WOOD_LIGHT);
        c.set(x, y + 3, WOOD_DARK);
        c.rect(x + 4, y + 1, 5, 2, METAL_DARK);  // cajón
        c.rect(x + 4, y + 1, 5, 1, METAL);
        c.rect(x + 9, y + 1, 3, 2, WOOD);        // guardamanos
        c.rect(x + 9, y + 1, 3, 1, WOOD_LIGHT);
        c.rect(x + 12, y + 1, 3, 1, METAL_DARK); // cañón
        c.set(x + 13, y, METAL_DARK);            // punto de mira
        c.rect(x + 7, y + 3, 2, 1, METAL_DARK);  // cargador curvo, hacia delante
        c.rect(x + 8, y + 4, 2, 1, METAL_DARK);
        c.rect(x + 9, y + 5, 2, 1, METAL_DARK);
        c.rect(x + 5, y + 3, 1, 2, WOOD_DARK);   // empuñadura
    }
}
