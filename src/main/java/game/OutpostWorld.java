package game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import items.Item;
import items.ItemKind;
import items.ItemManager;
import npc.NpcManager;
import player.Player;
import structs.Struct;
import world.Chunk;
import world.WorldManager;

/**
 * Mundo "Puesto avanzado".
 *
 * <ul>
 *   <li>Casa inicial (donde aparece el jugador) con armas y materiales, ventanas y un anillo de sacos terreros
 *       vigilado por humanos aliados.</li>
 *   <li>Caminos de tierra hacia el este (aldea y cementerio) y el sur (fuerte).</li>
 *   <li>Aldea en ruinas con civiles que huyen de los zombis y algo de botín.</li>
 *   <li>Fuerte militar con humanos agresivos y una armería con AK-47 y sniper.</li>
 *   <li>Cementerio infestado de zombis con un mausoleo.</li>
 *   <li>Ruinas al sureste y un pequeño secreto en la esquina del mapa.</li>
 * </ul>
 *
 * <p>Todo se diseña en casillas ({@link Struct#SIZE} píxeles); la casilla (x, y) está centrada en
 * (x * SIZE, y * SIZE). El mapa usa una semilla fija, así que siempre sale igual.
 */
final class OutpostWorld {
    private static final int TILE = (int) Struct.SIZE;
    private static final int SIZE_TILES = 120;
    private static final int CHUNK_TILES = 10;
    private static final long SEED = 2026;

    // Índices de TexturesManager.environmentTextures
    private static final int TEX_TEEMO = 0;
    private static final int TEX_DIRT = 1;
    private static final int TEX_WALL = 2;
    private static final int TEX_GRASS = 3;
    private static final int TEX_HALF_WALL = 4;

    // Índices de ItemManager.guns / buildables
    private static final int SHOTGUN = 1;
    private static final int SNIPER = 2;
    private static final int AK47 = 3;
    private static final int WALL_ITEM = 0;
    private static final int HALF_WALL_ITEM = 1;
    private static final int FLOOR_ITEM = 2;

    /** Ningún zombi aparece al crear el mundo a menos de esta distancia (en casillas) del jugador. */
    private static final int SAFE_RADIUS_TILES = 20;
    /** Por debajo de 600 px (rango de persecución) para que la oleada vaya a por el jugador directamente. */
    private static final float WAVE_RADIUS = 550;
    private static final int WAVE_SIZE = 16;

    private final Random random = new Random(SEED);
    private final int[][] ground = new int[SIZE_TILES][SIZE_TILES];
    private final Map<Long, ItemKind> solids = new HashMap<>();
    private final int spawnX = Math.round(Player.SPAWN_X / TILE);
    private final int spawnY = Math.round(Player.SPAWN_Y / TILE);

    private OutpostWorld() {
    }

    static void create() {
        Player.inventory.add(ItemManager.guns.getFirst().copy());

        for (int i = 0; i < SIZE_TILES; i += CHUNK_TILES) {
            for (int j = 0; j < SIZE_TILES; j += CHUNK_TILES) {
                WorldManager.chunks.add(new Chunk(i * TILE, j * TILE));
            }
        }

        OutpostWorld world = new OutpostWorld();
        world.layout();
        world.build();
        world.populate();
        System.out.println("Puesto avanzado: " + WorldManager.chunks.size() + " chunks, "
                + world.solids.size() + " muros, " + WorldManager.npcs.size() + " NPCs");
    }

    /** Oleada de zombis en círculo alrededor del jugador. Tecla P. */
    static void spawnWave() {
        var random = ThreadLocalRandom.current();
        for (int i = 0; i < WAVE_SIZE; i++) {
            double angle = 2 * Math.PI * i / WAVE_SIZE;
            float x = Player.x + (float) Math.cos(angle) * WAVE_RADIUS;
            float y = Player.y + (float) Math.sin(angle) * WAVE_RADIUS;
            if (!WorldManager.isSolidAt(x, y)) {
                NpcManager.spawnNpc(random.nextBoolean() ? NpcManager.ZOMBIE : NpcManager.ZOMBIE_ALT,
                        x, y, random.nextInt(360));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Diseño del mapa (solo rellena `ground` y `solids`)
    // ---------------------------------------------------------------------------------------------

    private void layout() {
        for (int[] column : ground) {
            Arrays.fill(column, TEX_GRASS);
        }
        roads();
        house();
        village();
        fort();
        cemetery();
        ruins();
        teemoShrine();
    }

    private void roads() {
        int cx = spawnX, cy = spawnY;
        // Sur: de la puerta de la casa hasta la altura del fuerte y luego al este hasta su puerta
        ground(cx - 1, cy + 6, cx + 1, 50, TEX_DIRT);
        ground(cx - 1, 48, 25, 50, TEX_DIRT);
        // Este: atraviesa la aldea y llega al cementerio
        ground(cx + 6, cy - 1, 91, cy + 1, TEX_DIRT);
        ground(89, cy - 1, 91, 13, TEX_DIRT);
    }

    /** Casa inicial centrada en el punto de aparición del jugador. */
    private void house() {
        int cx = spawnX, cy = spawnY;

        // Anillo de sacos terreros (medio muro: frena a los zombis pero deja pasar las balas)
        outline(cx - 8, cy - 8, cx + 8, cy + 8, ItemKind.HALF_WALL);
        open(cx - 1, cy + 8, cx + 1, cy + 8); // salida sur
        open(cx + 8, cy - 1, cx + 8, cy + 1); // salida este

        // Casa
        outline(cx - 5, cy - 5, cx + 5, cy + 5, ItemKind.WALL);
        ground(cx - 4, cy - 4, cx + 4, cy + 4, TEX_DIRT);
        open(cx - 1, cy + 5, cx + 1, cy + 5); // puerta
        // Ventanas para disparar desde dentro
        fill(cx - 1, cy - 5, cx + 1, cy - 5, ItemKind.HALF_WALL);
        fill(cx - 5, cy - 1, cx - 5, cy + 1, ItemKind.HALF_WALL);
        fill(cx + 5, cy - 1, cx + 5, cy + 1, ItemKind.HALF_WALL);
        // Banco de trabajo (rincón de los materiales)
        fill(cx + 2, cy + 2, cx + 3, cy + 2, ItemKind.HALF_WALL);
    }

    private void village() {
        hut(32, 1, 38, 6, 'S');
        hut(42, 1, 48, 6, 'S');
        hut(35, 12, 43, 17, 'N');
    }

    /** Casa pequeña con suelo de tierra, puerta de 3 casillas en el lado indicado y algún muro caído. */
    private void hut(int x0, int y0, int x1, int y1, char door) {
        outline(x0, y0, x1, y1, ItemKind.WALL);
        ground(x0 + 1, y0 + 1, x1 - 1, y1 - 1, TEX_DIRT);
        ruin(x0, y0, x1, y1, 0.15);
        int mid = (x0 + x1) / 2;
        int doorY = door == 'S' ? y1 : y0;
        open(mid - 1, doorY, mid + 1, doorY);
    }

    private void fort() {
        int x0 = 25, y0 = 40, x1 = 45, y1 = 58;
        outline(x0, y0, x1, y1, ItemKind.WALL);
        ground(x0 + 1, y0 + 1, x1 - 1, y1 - 1, TEX_DIRT);

        // Almenas: medio muro cada 3 casillas para que los soldados disparen hacia fuera
        for (int x = x0 + 2; x < x1 - 1; x += 3) {
            solid(x, y0, ItemKind.HALF_WALL);
            solid(x, y1, ItemKind.HALF_WALL);
        }
        for (int y = y0 + 2; y < y1 - 1; y += 3) {
            solid(x1, y, ItemKind.HALF_WALL);
        }

        // Torres en las esquinas
        fill(x0 - 1, y0 - 1, x0 + 1, y0 + 1, ItemKind.WALL);
        fill(x1 - 1, y0 - 1, x1 + 1, y0 + 1, ItemKind.WALL);
        fill(x0 - 1, y1 - 1, x0 + 1, y1 + 1, ItemKind.WALL);
        fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, ItemKind.WALL);

        open(x0, 48, x0, 50); // puerta oeste, al final del camino

        // Barracón / armería
        outline(36, 43, 43, 49, ItemKind.WALL);
        open(36, 45, 36, 47);

        // Coberturas en el patio
        fill(29, 45, 32, 45, ItemKind.HALF_WALL);
        fill(29, 53, 32, 53, ItemKind.HALF_WALL);
        fill(38, 53, 41, 53, ItemKind.HALF_WALL);
    }

    private void cemetery() {
        int x0 = 80, y0 = 14, x1 = 100, y1 = 34;
        outline(x0, y0, x1, y1, ItemKind.HALF_WALL);
        ground(x0 + 1, y0 + 1, x1 - 1, y1 - 1, TEX_DIRT);
        open(89, y0, 91, y0); // entrada norte, donde acaba el camino

        // Mausoleo
        outline(87, 21, 93, 27, ItemKind.WALL);
        open(89, 27, 91, 27);

        // Tumbas en filas, dejando libre el pasillo central y el mausoleo
        for (int x = 83; x < x1; x += 3) {
            for (int y = 17; y < y1; y += 3) {
                boolean aisle = x >= 88 && x <= 92;
                boolean nearMausoleum = x >= 86 && x <= 94 && y >= 20 && y <= 28;
                if (!aisle && !nearMausoleum) {
                    solid(x, y, ItemKind.HALF_WALL);
                }
            }
        }
    }

    /** Restos de muros sueltos (rectos o en L) al sureste. */
    private void ruins() {
        for (int i = 0; i < 20; i++) {
            int x = 70 + random.nextInt(31);
            int y = 65 + random.nextInt(36);
            ItemKind kind = random.nextInt(10) < 7 ? ItemKind.WALL : ItemKind.HALF_WALL;
            int length = 3 + random.nextInt(4);
            boolean horizontal = random.nextBoolean();
            if (horizontal) {
                fill(x, y, x + length - 1, y, kind);
            } else {
                fill(x, y, x, y + length - 1, kind);
            }
            if (random.nextBoolean()) { // segundo tramo en L
                int leg = 2 + random.nextInt(3);
                if (horizontal) {
                    fill(x, y + 1, x, y + leg, kind);
                } else {
                    fill(x + 1, y, x + leg, y, kind);
                }
            }
        }
    }

    private void teemoShrine() {
        outline(109, 109, 115, 115, ItemKind.HALF_WALL);
        open(111, 109, 113, 109);
        ground(111, 111, 113, 113, TEX_TEEMO);
    }

    // ---------------------------------------------------------------------------------------------
    // Construcción y población
    // ---------------------------------------------------------------------------------------------

    private void build() {
        for (int x = 0; x < SIZE_TILES; x++) {
            for (int y = 0; y < SIZE_TILES; y++) {
                WorldManager.build(new Struct(px(x), px(y), ItemKind.FLOOR, ground[x][y]));
            }
        }
        solids.forEach((key, kind) -> {
            int texture = kind == ItemKind.WALL ? TEX_WALL : TEX_HALF_WALL;
            WorldManager.build(new Struct(px(tileX(key)), px(tileY(key)), kind, texture));
        });
    }

    private void populate() {
        int cx = spawnX, cy = spawnY;

        // Casa inicial: armero al norte y materiales junto al banco de trabajo
        item(ItemManager.guns.get(SHOTGUN).copy(), cx - 3, cy - 3);
        item(ItemManager.guns.get(SNIPER).copy(), cx + 3, cy - 3);
        item(ItemManager.buildables.get(WALL_ITEM), cx + 2, cy + 3);
        item(ItemManager.buildables.get(HALF_WALL_ITEM), cx + 3, cy + 3);
        item(ItemManager.buildables.get(FLOOR_ITEM), cx + 4, cy + 3);
        item(ItemManager.medkit(), cx - 3, cy + 3);
        item(ItemManager.medkit(), cx - 2, cy + 3);
        // Guardias aliados entre la casa y los sacos terreros (solo disparan a zombis)
        human(NpcManager.HUMAN_REACTIVE, cx - 3, cy + 7);
        human(NpcManager.HUMAN_REACTIVE, cx + 3, cy + 7);
        human(NpcManager.HUMAN_REACTIVE, cx + 7, cy - 3);
        human(NpcManager.HUMAN_REACTIVE, cx + 7, cy + 3);

        // Aldea: civiles que huyen, un guardia en el camino y botín aleatorio
        item(ItemManager.randomGun().copy(), 35, 3);
        item(ItemManager.randomGun().copy(), 45, 3);
        item(ItemManager.randomGun().copy(), 39, 15);
        item(ItemManager.buildables.get(HALF_WALL_ITEM), 47, 2);
        item(ItemManager.medkit(), 33, 5);
        item(ItemManager.medkit(), 41, 14);
        human(NpcManager.HUMAN_PASSIVE, 33, 3);
        human(NpcManager.HUMAN_PASSIVE, 37, 4);
        human(NpcManager.HUMAN_PASSIVE, 43, 4);
        human(NpcManager.HUMAN_PASSIVE, 47, 4);
        human(NpcManager.HUMAN_PASSIVE, 37, 14);
        human(NpcManager.HUMAN_PASSIVE, 41, 15);
        human(NpcManager.HUMAN_REACTIVE, 40, spawnY);
        zombies(6, 60, 0, 70, 20);

        // Fuerte: soldados agresivos (también disparan al jugador) y la armería
        item(ItemManager.guns.get(AK47).copy(), 39, 45);
        item(ItemManager.guns.get(AK47).copy(), 41, 45);
        item(ItemManager.guns.get(SNIPER).copy(), 40, 47);
        item(ItemManager.buildables.get(WALL_ITEM), 42, 47);
        item(ItemManager.medkit(), 38, 47);
        item(ItemManager.medkit(), 42, 44);
        human(NpcManager.HUMAN_AGGRESSIVE, 30, 43);
        human(NpcManager.HUMAN_AGGRESSIVE, 34, 49);
        human(NpcManager.HUMAN_AGGRESSIVE, 28, 49);
        human(NpcManager.HUMAN_AGGRESSIVE, 30, 55);
        human(NpcManager.HUMAN_AGGRESSIVE, 40, 55);
        zombies(10, 25, 70, 45, 78);

        // Cementerio: la horda, y un premio dentro del mausoleo
        item(ItemManager.guns.get(SNIPER).copy(), 90, 24);
        item(ItemManager.randomGun().copy(), 89, 23);
        item(ItemManager.medkit(), 91, 23);
        item(ItemManager.medkit(), 91, 25);
        zombies(25, 81, 15, 99, 33);

        // Ruinas
        item(ItemManager.randomGun().copy(), 75, 80);
        item(ItemManager.randomGun().copy(), 95, 95);
        zombies(15, 70, 65, 105, 105);

        // Secreto
        item(ItemManager.guns.get(AK47).copy(), 112, 112);
        item(ItemManager.medkit(), 111, 113);
        item(ItemManager.medkit(), 113, 113);

        // Zombis sueltos por todo el mapa
        zombies(30, 0, 0, SIZE_TILES - 1, SIZE_TILES - 1);
    }

    private void item(Item item, int x, int y) {
        WorldManager.dropItem(item, px(x), px(y));
    }

    private void human(int type, int x, int y) {
        if (isFree(x, y)) {
            NpcManager.spawnNpc(type, px(x), px(y), random.nextInt(360));
        }
    }

    /** Hasta {@code count} zombis en casillas libres del rectángulo, lejos del punto de aparición. */
    private void zombies(int count, int x0, int y0, int x1, int y1) {
        int placed = 0;
        for (int attempt = 0; attempt < count * 20 && placed < count; attempt++) {
            int x = x0 + random.nextInt(x1 - x0 + 1);
            int y = y0 + random.nextInt(y1 - y0 + 1);
            int dx = x - spawnX, dy = y - spawnY;
            if (isFree(x, y) && dx * dx + dy * dy > SAFE_RADIUS_TILES * SAFE_RADIUS_TILES) {
                int type = random.nextBoolean() ? NpcManager.ZOMBIE : NpcManager.ZOMBIE_ALT;
                NpcManager.spawnNpc(type, px(x), px(y), random.nextInt(360));
                placed++;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades de casillas
    // ---------------------------------------------------------------------------------------------

    private static float px(int tile) {
        return tile * TILE;
    }

    private static boolean inWorld(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE_TILES && y < SIZE_TILES;
    }

    private static long key(int x, int y) {
        return (long) x * SIZE_TILES + y;
    }

    private static int tileX(long key) {
        return (int) (key / SIZE_TILES);
    }

    private static int tileY(long key) {
        return (int) (key % SIZE_TILES);
    }

    private boolean isFree(int x, int y) {
        return inWorld(x, y) && !solids.containsKey(key(x, y));
    }

    private void solid(int x, int y, ItemKind kind) {
        if (inWorld(x, y)) {
            solids.put(key(x, y), kind);
        }
    }

    private void fill(int x0, int y0, int x1, int y1, ItemKind kind) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                solid(x, y, kind);
            }
        }
    }

    private void outline(int x0, int y0, int x1, int y1, ItemKind kind) {
        fill(x0, y0, x1, y0, kind);
        fill(x0, y1, x1, y1, kind);
        fill(x0, y0, x0, y1, kind);
        fill(x1, y0, x1, y1, kind);
    }

    private void open(int x0, int y0, int x1, int y1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                solids.remove(key(x, y));
            }
        }
    }

    /** Quita al azar algunas casillas del contorno para que parezca derruido. */
    private void ruin(int x0, int y0, int x1, int y1, double chance) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                boolean border = x == x0 || x == x1 || y == y0 || y == y1;
                if (border && random.nextDouble() < chance) {
                    solids.remove(key(x, y));
                }
            }
        }
    }

    private void ground(int x0, int y0, int x1, int y1, int texture) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (inWorld(x, y)) {
                    ground[x][y] = texture;
                }
            }
        }
    }
}
