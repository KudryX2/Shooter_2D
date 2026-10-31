package tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * Genera los gráficos provisionales en pixel art de suelos, estructuras y personajes ({@link CharacterArt}) con la
 * tarea {@code generateArt} de Gradle.
 * Siempre salen iguales (semillas fijas). Cuando haya gráficos de verdad basta con sustituir los PNG.
 *
 * <p>Escala: 1 píxel del dibujo = 4 unidades del mundo, así que una casilla ({@code Struct.SIZE} = 64) son 16 px.
 * Los muros se ven en 3/4: arriba el remate (lo que se ve desde encima) y debajo la cara frontal. Sus medidas
 * tienen que cuadrar con {@code StructKind} (grosor) y {@code graphics.WorldRenderer} (altura):
 * <ul>
 *   <li>Muro: 4 px de grosor (16) y 12 px de alto (48).</li>
 *   <li>Saco terrero: 3 px de grosor (12) y 6 px de alto (24).</li>
 *   <li>Largo de un muro: una casilla más su grosor (para que las esquinas se solapen).</li>
 * </ul>
 */
public final class PixelArtGenerator {
    private static final int TILE = 16;
    private static final int WALL_THICKNESS = 4, WALL_HEIGHT = 12;
    private static final int SANDBAG_THICKNESS = 3, SANDBAG_HEIGHT = 6;
    /** Caja de madera: ancho y fondo de lo que ocupa (12x8 px = 48x32) y alto de su frente (8 px = 32). */
    private static final int CRATE_WIDTH = 12, CRATE_DEPTH = 8, CRATE_HEIGHT = 8;
    private static final int CRATE_LIGHT = 0xb68a56, CRATE_PLANK = 0x9c7242, CRATE_GAP = 0x6e4c2a;
    private static final int CRATE_FRONT = 0x8a6238, CRATE_FRONT_DARK = 0x6a4826, CRATE_EDGE = 0x3a2614;

    // Paletas
    private static final int[] GRASS = {0x4a8b3b, 0x4a8b3b, 0x4a8b3b, 0x478638, 0x4f913f, 0x437d35};
    private static final int GRASS_DARK = 0x33652a, GRASS_LIGHT = 0x6aae52;
    private static final int[] DIRT = {0x7a5634, 0x85603b, 0x85603b, 0x6e4c2e, 0x8d6841};
    private static final int PEBBLE = 0x9b8b78, PEBBLE_SHADOW = 0x5e4430;
    private static final int[] FLOWERS = {0xe8d35a, 0xf0efe0, 0xd96a6a};

    private static final int STONE_LIGHT = 0xbdb8ae, STONE = 0xa39e94, STONE_DARK = 0x77716a, STONE_EDGE = 0x5a5550;
    private static final int[] BRICKS = {0x9c4a36, 0xa8553e, 0x8f4331, 0xa04e3a};
    private static final int MORTAR = 0x6b5a4e, BRICK_SHADOW = 0x5a2a20, BRICK_LIGHT = 0xb8664c;

    private static final int BAG = 0xb49a66, BAG_LIGHT = 0xcdb683, BAG_SHADOW = 0x8a7348, BAG_SEAM = 0x6e5a38;

    private final File outputDir;

    private PixelArtGenerator(File outputDir) {
        this.outputDir = outputDir;
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("No se puede crear " + dir);
        }
        new PixelArtGenerator(dir).generateAll();
    }

    private void generateAll() throws IOException {
        for (int i = 0; i < 3; i++) {
            save("3-grass_" + i, grass(i));
        }
        for (int i = 0; i < 2; i++) {
            save("3-dirt_" + i, dirt(i));
        }
        int wallLength = TILE + WALL_THICKNESS;
        save("3-wall-h", wall(wallLength, WALL_THICKNESS, false));
        save("3-wall-v", wall(WALL_THICKNESS, wallLength, true));
        save("3-wall", wallIcon());
        int bagLength = TILE + SANDBAG_THICKNESS;
        save("3-halfWall-h", sandbags(bagLength, SANDBAG_THICKNESS, false));
        save("3-halfWall-v", sandbags(SANDBAG_THICKNESS, bagLength, true));
        save("3-halfWall", sandbagIcon());
        save("3-floor", floorIcon());
        save("3-crate", crate());
        save("3-crateIcon", crateIcon());

        save("2-pointer", crosshair());
        save("0-bullet", ItemArt.bullet());
        save("2-hp", ItemArt.healthFill(0x7ee06a, 0x45b03a));
        save("2-hp1", ItemArt.healthFill(0xf2d46a, 0xd1a62c));
        save("2-hp2", ItemArt.healthFill(0xf07060, 0xc8352a));
        save("2-hpFont", ItemArt.healthFill(0x3a3634, 0x262220));
        save("2-normalCell", ItemArt.cell());
        save("2-selectedCell", ItemArt.selectedCell());
        save("4-blood", ItemArt.blood());
        save("7-medkit", ItemArt.medkit());
        for (int n = 0; n <= 9; n++) {
            save("2-digit_" + n, ItemArt.digit(n));
        }
        save("2-slash", ItemArt.glyph('/'));
        save("2-parenOpen", ItemArt.glyph('('));
        save("2-parenClose", ItemArt.glyph(')'));
        save("8-ammoPistol", ItemArt.ammoBox(0xc9a227, 0xe6c34a, 0xd8b04a));
        save("8-ammoShotgun", ItemArt.ammoBox(0xb03a2e, 0xd0584a, 0xc84a3a));
        save("8-ammoRifle", ItemArt.ammoBox(0x5a7a3a, 0x7a9a52, 0xd8b04a));
        save("8-ammoSniper", ItemArt.ammoBox(0x34507a, 0x4a6a9a, 0xb8bcc4));
        for (GunArt.Gun gun : GunArt.Gun.values()) {
            save("5-" + gun.file, GunArt.icon(gun));
            save("6-" + gun.file, GunArt.held(gun));
        }
        for (MeleeArt.Melee melee : MeleeArt.Melee.values()) {
            save("5-" + melee.file, MeleeArt.icon(melee));
            save("6-" + melee.file, MeleeArt.held(melee));
        }

        characters("1-player", CharacterArt.PLAYER, false);
        characters("4-human", CharacterArt.HUMAN, true);
        characters("4-zombie", CharacterArt.ZOMBIE, false);
        characters("4-zombieAlt", CharacterArt.ZOMBIE_ALT, false);
    }

    /**
     * Cruceta de 13x13: cuatro brazos blancos con un hueco y un punto en el centro exacto (adonde se apunta), con
     * contorno oscuro para que se vea sobre cualquier fondo. Se dibuja a 2 px de pantalla por píxel.
     */
    private static BufferedImage crosshair() {
        Canvas c = new Canvas(13, 13);
        int white = 0xf4f4ee, center = 6;
        c.rect(1, center, 3, 1, white);
        c.rect(9, center, 3, 1, white);
        c.rect(center, 1, 1, 3, white);
        c.rect(center, 9, 1, 3, white);
        c.set(center, center, white);
        return c.outlined();
    }

    /** {@code nombre-idle-dir.png} y {@code nombre-walk-dir_0..3.png} por dirección; tumbado si {@code lying}. */
    private void characters(String name, CharacterArt.Palette palette, boolean lying) throws IOException {
        for (String direction : CharacterArt.DIRECTIONS) {
            save(name + "-idle-" + direction, CharacterArt.frame(palette, direction, -1));
            for (int frame = 0; frame < CharacterArt.WALK_FRAMES; frame++) {
                save(name + "-walk-" + direction + "_" + frame, CharacterArt.frame(palette, direction, frame));
            }
        }
        if (lying) {
            save(name + "-lying", CharacterArt.lying(palette));
        }
    }

    private void save(String name, BufferedImage image) throws IOException {
        File file = new File(outputDir, name + ".png");
        ImageIO.write(image, "png", file);
        System.out.println("Generado " + file.getName() + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }

    // ---------------------------------------------------------------------------------------------
    // Suelos (16x16, se repiten sin cortes: lo que sale por un borde entra por el contrario)
    // ---------------------------------------------------------------------------------------------

    private static BufferedImage grass(int variant) {
        Random random = new Random(100 + variant);
        BufferedImage image = noise(GRASS, random);
        for (int i = 0; i < 6; i++) { // matas: dos píxeles oscuros con la punta clara
            int x = random.nextInt(TILE), y = random.nextInt(TILE);
            set(image, x, y, GRASS_DARK);
            set(image, x, y + 1, GRASS_DARK);
            set(image, x, y - 1, GRASS_LIGHT);
        }
        if (variant == 1) { // flores
            for (int i = 0; i < 3; i++) {
                int x = random.nextInt(TILE), y = random.nextInt(TILE);
                set(image, x, y, FLOWERS[random.nextInt(FLOWERS.length)]);
                set(image, x, y + 1, GRASS_DARK);
            }
        } else if (variant == 2) { // alguna piedrecita
            pebble(image, random.nextInt(TILE), random.nextInt(TILE));
        }
        return image;
    }

    private static BufferedImage dirt(int variant) {
        Random random = new Random(200 + variant);
        BufferedImage image = noise(DIRT, random);
        for (int i = 0; i < 2 + variant; i++) {
            pebble(image, random.nextInt(TILE), random.nextInt(TILE));
        }
        return image;
    }

    private static BufferedImage noise(int[] palette, Random random) {
        BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < TILE; x++) {
            for (int y = 0; y < TILE; y++) {
                set(image, x, y, palette[random.nextInt(palette.length)]);
            }
        }
        return image;
    }

    private static void pebble(BufferedImage image, int x, int y) {
        set(image, x, y, PEBBLE);
        set(image, x + 1, y, PEBBLE);
        set(image, x, y + 1, PEBBLE_SHADOW);
        set(image, x + 1, y + 1, PEBBLE_SHADOW);
    }

    // ---------------------------------------------------------------------------------------------
    // Muros de ladrillo con remate de piedra
    // ---------------------------------------------------------------------------------------------

    /**
     * Muro visto en 3/4: el remate ocupa la base (width x depth) y debajo va la cara frontal de
     * {@link #WALL_HEIGHT} px. {@code vertical}: la base es alta y estrecha (muro norte-sur).
     */
    private static BufferedImage wall(int width, int depth, boolean vertical) {
        BufferedImage image = new BufferedImage(width, depth + WALL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        stoneCap(image, 0, 0, width, depth, vertical);
        bricks(image, 0, depth, width, WALL_HEIGHT, new Random(vertical ? 301 : 300));
        return image;
    }

    /** Remate: claro por arriba/izquierda (luz del noroeste), oscuro en el borde que da a la cara frontal. */
    private static void stoneCap(BufferedImage image, int x0, int y0, int width, int depth, boolean vertical) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < depth; y++) {
                int color = STONE;
                if (y == depth - 1) {
                    color = STONE_EDGE;
                } else if ((y == 0 && !vertical) || (vertical && x == 0)) {
                    color = STONE_LIGHT;
                } else if (vertical && x == width - 1) {
                    color = STONE_DARK;
                } else if (!vertical && (x + y) % 7 == 0) {
                    color = STONE_DARK; // juntas de las losas
                }
                set(image, x0 + x, y0 + y, color);
            }
        }
    }

    /** Hiladas de ladrillo de 2 px con junta de 1 px, ladrillos de 5 px alternados. */
    private static void bricks(BufferedImage image, int x0, int y0, int width, int height, Random random) {
        int[][] brickColors = new int[height / 3 + 1][width / 5 + 3]; // un color por ladrillo
        for (int[] course : brickColors) {
            for (int i = 0; i < course.length; i++) {
                course[i] = BRICKS[random.nextInt(BRICKS.length)];
            }
        }
        for (int y = 0; y < height; y++) {
            int course = y / 3;
            boolean mortarRow = y % 3 == 2;
            int offset = course % 2 == 0 ? 0 : 3;
            for (int x = 0; x < width; x++) {
                int column = x + offset;
                int color;
                if (mortarRow || column % 5 == 4) {
                    color = MORTAR;
                } else {
                    color = brickColors[course][column / 5];
                    if (y % 3 == 0) {
                        color = mix(color, BRICK_LIGHT, 0.35f); // borde superior iluminado
                    }
                }
                if (y == height - 1) {
                    color = BRICK_SHADOW; // contacto con el suelo
                } else if (y == 0) {
                    color = mix(color, BRICK_SHADOW, 0.4f); // sombra bajo el remate
                }
                set(image, x0 + x, y0 + y, color);
            }
        }
    }

    private static BufferedImage wallIcon() {
        BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        stoneCap(image, 1, 2, 14, 3, false);
        bricks(image, 1, 5, 14, 9, new Random(302));
        return image;
    }

    // ---------------------------------------------------------------------------------------------
    // Sacos terreros
    // ---------------------------------------------------------------------------------------------

    private static BufferedImage sandbags(int width, int depth, boolean vertical) {
        BufferedImage image = new BufferedImage(width, depth + SANDBAG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        bagTops(image, 0, 0, width, depth, vertical);
        bagFront(image, 0, depth, width, SANDBAG_HEIGHT);
        return image;
    }

    /** Parte de arriba: sacos tumbados uno detrás de otro, con costuras cada 5 px. */
    private static void bagTops(BufferedImage image, int x0, int y0, int width, int depth, boolean vertical) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < depth; y++) {
                int along = vertical ? y : x;
                int color = BAG;
                if (along % 5 == 4) {
                    color = BAG_SEAM;
                } else if ((y == 0 && !vertical) || (vertical && x == 0)) {
                    color = BAG_LIGHT;
                } else if (y == depth - 1 && !vertical) {
                    color = BAG_SHADOW;
                }
                set(image, x0 + x, y0 + y, color);
            }
        }
    }

    /** Cara frontal: dos filas de sacos de 3 px, alternados, redondeados en las esquinas. */
    private static void bagFront(BufferedImage image, int x0, int y0, int width, int height) {
        for (int y = 0; y < height; y++) {
            int row = y / 3, inBag = y % 3;
            int offset = row % 2 == 0 ? 0 : 2;
            for (int x = 0; x < width; x++) {
                int along = (x + offset) % 5;
                int color;
                if (along == 4) {
                    color = BAG_SEAM;
                } else if (inBag == 0) {
                    color = along == 0 || along == 3 ? BAG : BAG_LIGHT;
                } else if (inBag == 2) {
                    color = along == 0 || along == 3 ? BAG_SEAM : BAG_SHADOW;
                } else {
                    color = BAG;
                }
                set(image, x0 + x, y0 + y, color);
            }
        }
    }

    private static BufferedImage sandbagIcon() {
        BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        bagTops(image, 1, 5, 14, 3, false);
        bagFront(image, 1, 8, 14, 6);
        return image;
    }

    /**
     * Icono del material de suelo: un bloque de césped visto en 3/4 (arriba el césped y delante el borde de tierra),
     * con margen y contorno como los demás iconos. No es la textura que se construye, que ocupa la casilla entera.
     */
    /**
     * Caja de madera en 3/4: arriba la tapa (tablas vistas desde encima) y debajo el frente, con el borde oscuro dentro
     * de la imagen para que mida justo lo que ocupa ({@code StructKind.CRATE}) más su alto.
     */
    private static BufferedImage crate() {
        Canvas c = new Canvas(CRATE_WIDTH, CRATE_DEPTH + CRATE_HEIGHT);
        crateBox(c, 0, 0, CRATE_WIDTH, CRATE_DEPTH, CRATE_HEIGHT);
        return c.image();
    }

    /** Icono de la caja: la misma, más pequeña (10 px de ancho, 6 de tapa y 5 de frente) y con contorno. */
    private static BufferedImage crateIcon() {
        Canvas c = new Canvas(TILE, TILE);
        crateBox(c, 3, 3, 10, 6, 5);
        return c.outlined();
    }

    /** Caja de {@code width} de ancho, con la tapa de {@code depth} filas y el frente de {@code height}. */
    private static void crateBox(Canvas c, int x, int y, int width, int depth, int height) {
        // Tapa: tablas de 3 filas separadas por una raya oscura, con la de arriba iluminada
        for (int row = 0; row < depth; row++) {
            int color = row % 3 == 2 ? CRATE_GAP : row % 3 == 0 ? CRATE_LIGHT : CRATE_PLANK;
            c.rect(x, y + row, width, 1, color);
        }
        // Frente: más oscuro, con una tabla cruzada en diagonal
        int front = y + depth;
        c.rect(x, front, width, height, CRATE_FRONT);
        c.rect(x, front + height - 1, width, 1, CRATE_FRONT_DARK);
        for (int i = 1; i < width - 1; i++) {
            c.set(x + i, front + 1 + (i - 1) * (height - 2) / (width - 2), CRATE_FRONT_DARK);
        }
        // Borde: marco oscuro alrededor y la arista entre la tapa y el frente
        c.rect(x, y, width, 1, CRATE_EDGE);
        c.rect(x, y + depth + height - 1, width, 1, CRATE_EDGE);
        c.rect(x, y, 1, depth + height, CRATE_EDGE);
        c.rect(x + width - 1, y, 1, depth + height, CRATE_EDGE);
        c.rect(x, front, width, 1, CRATE_EDGE);
    }

    private static BufferedImage floorIcon() {
        Canvas c = new Canvas(TILE, TILE);
        Random random = new Random(400);
        for (int x = 2; x < 14; x++) {
            for (int y = 3; y < 11; y++) {
                c.set(x, y, GRASS[random.nextInt(GRASS.length)]);
            }
        }
        for (int i = 0; i < 4; i++) { // matas
            int x = 3 + random.nextInt(10), y = 5 + random.nextInt(5);
            c.set(x, y, GRASS_DARK);
            c.set(x, y - 1, GRASS_LIGHT);
        }
        c.rect(2, 3, 12, 1, GRASS_LIGHT);   // borde de arriba, iluminado
        c.rect(2, 11, 12, 2, DIRT[0]);      // cara de delante: tierra
        c.rect(2, 12, 12, 1, DIRT[3]);
        return c.outlined();
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------------

    /** Pinta un píxel opaco; las coordenadas fuera de la imagen dan la vuelta (para que los suelos encajen). */
    private static void set(BufferedImage image, int x, int y, int rgb) {
        int w = image.getWidth(), h = image.getHeight();
        image.setRGB(Math.floorMod(x, w), Math.floorMod(y, h), 0xFF00_0000 | rgb);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
