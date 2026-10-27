package game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import assets.TextureId;
import items.Consumable;
import items.Gun;
import items.GunType;
import items.Item;
import items.StructItem;
import npc.NpcManager;
import npc.NpcType;
import player.Player;
import structs.Struct;
import structs.StructKind;
import world.Chunk;
import world.Structures;
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
 * (x * SIZE, y * SIZE). Los muros van sobre los bordes de las casillas: cada casilla tiene un borde norte y uno
 * oeste. Los edificios se definen por sus casillas interiores. El mapa usa una semilla fija, así que siempre
 * sale igual.
 */
final class OutpostWorld {
    private static final int TILE = (int) Struct.SIZE;
    private static final int SIZE_TILES = 120;
    private static final int CHUNK_TILES = 10;
    private static final long SEED = 2026;

    /** Ningún zombi aparece al crear el mundo a menos de esta distancia (en casillas) del jugador. */
    private static final int SAFE_RADIUS_TILES = 20;
    /** Por debajo de 600 px (rango de persecución) para que la oleada vaya a por el jugador directamente. */
    private static final float WAVE_RADIUS = 550;
    private static final int WAVE_SIZE = 16;

    private final Random random = new Random(SEED);
    private final TextureId[][] ground = new TextureId[SIZE_TILES][SIZE_TILES];
    /** Muros por borde (ver {@link #edgeKey}). */
    private final Map<Long, StructKind> walls = new HashMap<>();
    private final int spawnX = Math.round(Player.SPAWN_X / TILE);
    private final int spawnY = Math.round(Player.SPAWN_Y / TILE);

    private OutpostWorld() {
    }

    static void create() {
        Player.inventory.add(new Gun(GunType.PISTOL));

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
                + world.walls.size() + " muros, " + WorldManager.npcs.size() + " NPCs");
    }

    /** Oleada de zombis en círculo alrededor del jugador. Tecla P. */
    static void spawnWave() {
        var random = ThreadLocalRandom.current();
        for (int i = 0; i < WAVE_SIZE; i++) {
            double angle = 2 * Math.PI * i / WAVE_SIZE;
            float x = Player.x + (float) Math.cos(angle) * WAVE_RADIUS;
            float y = Player.y + (float) Math.sin(angle) * WAVE_RADIUS;
            if (!Structures.isSolidAt(x, y)) {
                NpcManager.spawn(random.nextBoolean() ? NpcType.ZOMBIE : NpcType.ZOMBIE_ALT,
                        x, y, random.nextInt(360));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Diseño del mapa (solo rellena `ground` y `walls`)
    // ---------------------------------------------------------------------------------------------

    private void layout() {
        for (TextureId[] column : ground) {
            Arrays.fill(column, TextureId.GRASS);
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
        ground(cx - 1, cy + 6, cx + 1, 50, TextureId.DIRT);
        ground(cx - 1, 48, 25, 50, TextureId.DIRT);
        // Este: atraviesa la aldea y llega al cementerio
        ground(cx + 6, cy - 1, 91, cy + 1, TextureId.DIRT);
        ground(89, cy - 1, 91, 13, TextureId.DIRT);
    }

    /** Casa inicial centrada en el punto de aparición del jugador. */
    private void house() {
        int cx = spawnX, cy = spawnY;

        // Anillo de sacos terreros (medio muro: frena a los zombis pero deja pasar las balas)
        room(cx - 7, cy - 7, cx + 7, cy + 7, StructKind.HALF_WALL);
        northWalls(cx - 1, cx + 1, cy + 8, null); // salida sur
        westWalls(cx + 8, cy - 1, cy + 1, null);  // salida este

        // Casa
        room(cx - 4, cy - 4, cx + 4, cy + 4, StructKind.WALL);
        ground(cx - 4, cy - 4, cx + 4, cy + 4, TextureId.DIRT);
        northWalls(cx - 1, cx + 1, cy + 5, null); // puerta
        // Ventanas para disparar desde dentro
        northWalls(cx - 1, cx + 1, cy - 4, StructKind.HALF_WALL);
        westWalls(cx - 4, cy - 1, cy + 1, StructKind.HALF_WALL);
        westWalls(cx + 5, cy - 1, cy + 1, StructKind.HALF_WALL);
        // Banco de trabajo (rincón de los materiales)
        northWalls(cx + 2, cx + 3, cy + 2, StructKind.HALF_WALL);
    }

    private void village() {
        hut(33, 2, 37, 5, 'S');
        hut(43, 2, 47, 5, 'S');
        hut(36, 13, 42, 16, 'N');
    }

    /** Casa pequeña (casillas interiores) con suelo de tierra, puerta de 3 casillas y algún muro caído. */
    private void hut(int x0, int y0, int x1, int y1, char door) {
        room(x0, y0, x1, y1, StructKind.WALL);
        ground(x0, y0, x1, y1, TextureId.DIRT);
        ruin(x0, y0, x1, y1, 0.15);
        int mid = (x0 + x1) / 2;
        int doorY = door == 'S' ? y1 + 1 : y0;
        northWalls(mid - 1, mid + 1, doorY, null);
    }

    private void fort() {
        int x0 = 26, y0 = 41, x1 = 44, y1 = 57;
        room(x0, y0, x1, y1, StructKind.WALL);
        ground(x0, y0, x1, y1, TextureId.DIRT);

        // Almenas: medio muro cada 3 casillas para que los soldados disparen hacia fuera
        for (int x = x0 + 1; x < x1; x += 3) {
            northWalls(x, x, y0, StructKind.HALF_WALL);
            northWalls(x, x, y1 + 1, StructKind.HALF_WALL);
        }
        for (int y = y0 + 1; y < y1; y += 3) {
            westWalls(x1 + 1, y, y, StructKind.HALF_WALL);
        }

        // Torres en las esquinas: un cuadrado de 2x2 casillas sobre cada esquina
        room(x0 - 1, y0 - 1, x0, y0, StructKind.WALL);
        room(x1, y0 - 1, x1 + 1, y0, StructKind.WALL);
        room(x0 - 1, y1, x0, y1 + 1, StructKind.WALL);
        room(x1, y1, x1 + 1, y1 + 1, StructKind.WALL);

        westWalls(x0, 48, 50, null); // puerta oeste, al final del camino

        // Barracón / armería
        room(37, 44, 42, 48, StructKind.WALL);
        westWalls(37, 45, 47, null);

        // Coberturas en el patio
        northWalls(29, 32, 45, StructKind.HALF_WALL);
        northWalls(29, 32, 53, StructKind.HALF_WALL);
        northWalls(38, 41, 53, StructKind.HALF_WALL);
    }

    private void cemetery() {
        int x0 = 81, y0 = 15, x1 = 99, y1 = 33;
        room(x0, y0, x1, y1, StructKind.HALF_WALL);
        ground(x0, y0, x1, y1, TextureId.DIRT);
        northWalls(89, 91, y0, null); // entrada norte, donde acaba el camino

        // Mausoleo
        room(88, 22, 92, 26, StructKind.WALL);
        northWalls(89, 91, 27, null);

        // Tumbas en filas, dejando libre el pasillo central y el mausoleo
        for (int x = 83; x <= x1; x += 3) {
            for (int y = 17; y <= y1; y += 3) {
                boolean aisle = x >= 88 && x <= 92;
                boolean nearMausoleum = x >= 86 && x <= 94 && y >= 20 && y <= 28;
                if (!aisle && !nearMausoleum) {
                    northWalls(x, x, y, StructKind.HALF_WALL);
                }
            }
        }
    }

    /** Restos de muros sueltos (rectos o en L) al sureste. */
    private void ruins() {
        for (int i = 0; i < 20; i++) {
            int x = 70 + random.nextInt(31);
            int y = 65 + random.nextInt(36);
            StructKind kind = random.nextInt(10) < 7 ? StructKind.WALL : StructKind.HALF_WALL;
            int length = 3 + random.nextInt(4);
            boolean horizontal = random.nextBoolean();
            if (horizontal) {
                northWalls(x, x + length - 1, y, kind);
            } else {
                westWalls(x, y, y + length - 1, kind);
            }
            if (random.nextBoolean()) { // segundo tramo en L, desde la misma esquina
                int leg = 2 + random.nextInt(3);
                if (horizontal) {
                    westWalls(x, y, y + leg - 1, kind);
                } else {
                    northWalls(x, x + leg - 1, y, kind);
                }
            }
        }
    }

    private void teemoShrine() {
        room(110, 110, 114, 114, StructKind.HALF_WALL);
        northWalls(111, 113, 110, null);
        ground(111, 111, 113, 113, TextureId.TEEMO);
    }

    // ---------------------------------------------------------------------------------------------
    // Construcción y población
    // ---------------------------------------------------------------------------------------------

    private void build() {
        for (int x = 0; x < SIZE_TILES; x++) {
            for (int y = 0; y < SIZE_TILES; y++) {
                Structures.build(new Struct(px(x), px(y), StructKind.FLOOR, ground[x][y]));
            }
        }
        walls.forEach((key, kind) -> {
            int x = edgeX(key), y = edgeY(key);
            Structures.build(isVertical(key)
                    ? Struct.westWall(x, y, kind)
                    : Struct.northWall(x, y, kind));
        });
    }

    private void populate() {
        int cx = spawnX, cy = spawnY;

        // Casa inicial: armero al norte y materiales junto al banco de trabajo
        item(new Gun(GunType.SHOTGUN), cx - 3, cy - 3);
        item(new Gun(GunType.SNIPER), cx + 3, cy - 3);
        item(new StructItem(StructKind.WALL), cx + 2, cy + 3);
        item(new StructItem(StructKind.HALF_WALL), cx + 3, cy + 3);
        item(new StructItem(StructKind.FLOOR), cx + 4, cy + 3);
        item(Consumable.medkit(), cx - 3, cy + 3);
        item(Consumable.medkit(), cx - 2, cy + 3);
        // Guardias aliados entre la casa y los sacos terreros (solo disparan a zombis)
        human(NpcType.GUARD, cx - 3, cy + 7);
        human(NpcType.GUARD, cx + 3, cy + 7);
        human(NpcType.GUARD, cx + 7, cy - 3);
        human(NpcType.GUARD, cx + 7, cy + 3);

        // Aldea: civiles que huyen, un guardia en el camino y botín aleatorio
        item(new Gun(GunType.random()), 35, 3);
        item(new Gun(GunType.random()), 45, 3);
        item(new Gun(GunType.random()), 39, 15);
        item(new StructItem(StructKind.HALF_WALL), 47, 2);
        item(Consumable.medkit(), 33, 5);
        item(Consumable.medkit(), 41, 14);
        human(NpcType.CIVILIAN, 33, 3);
        human(NpcType.CIVILIAN, 37, 4);
        human(NpcType.CIVILIAN, 43, 4);
        human(NpcType.CIVILIAN, 47, 4);
        human(NpcType.CIVILIAN, 37, 14);
        human(NpcType.CIVILIAN, 41, 15);
        human(NpcType.GUARD, 40, spawnY);
        zombies(6, 60, 0, 70, 20);

        // Fuerte: soldados agresivos (también disparan al jugador) y la armería
        item(new Gun(GunType.ASSAULT_RIFLE), 39, 45);
        item(new Gun(GunType.ASSAULT_RIFLE), 41, 45);
        item(new Gun(GunType.SNIPER), 40, 47);
        item(new StructItem(StructKind.WALL), 42, 47);
        item(Consumable.medkit(), 38, 47);
        item(Consumable.medkit(), 42, 44);
        human(NpcType.SOLDIER, 30, 43);
        human(NpcType.SOLDIER, 34, 49);
        human(NpcType.SOLDIER, 28, 49);
        human(NpcType.SOLDIER, 30, 55);
        human(NpcType.SOLDIER, 40, 55);
        zombies(10, 25, 70, 45, 78);

        // Cementerio: la horda, y un premio dentro del mausoleo
        item(new Gun(GunType.SNIPER), 90, 24);
        item(new Gun(GunType.random()), 89, 23);
        item(Consumable.medkit(), 91, 23);
        item(Consumable.medkit(), 91, 25);
        zombies(25, 81, 15, 99, 33);

        // Ruinas
        item(new Gun(GunType.random()), 75, 80);
        item(new Gun(GunType.random()), 95, 95);
        zombies(15, 70, 65, 105, 105);

        // Secreto
        item(new Gun(GunType.ASSAULT_RIFLE), 112, 112);
        item(Consumable.medkit(), 111, 113);
        item(Consumable.medkit(), 113, 113);

        // Zombis sueltos por todo el mapa
        zombies(30, 0, 0, SIZE_TILES - 1, SIZE_TILES - 1);
    }

    private void item(Item item, int x, int y) {
        WorldManager.dropItem(item, px(x), px(y));
    }

    private void human(NpcType type, int x, int y) {
        if (inWorld(x, y)) {
            NpcManager.spawn(type, px(x), px(y), random.nextInt(360));
        }
    }

    /** Hasta {@code count} zombis en casillas del rectángulo, lejos del punto de aparición. */
    private void zombies(int count, int x0, int y0, int x1, int y1) {
        int placed = 0;
        for (int attempt = 0; attempt < count * 20 && placed < count; attempt++) {
            int x = x0 + random.nextInt(x1 - x0 + 1);
            int y = y0 + random.nextInt(y1 - y0 + 1);
            int dx = x - spawnX, dy = y - spawnY;
            if (inWorld(x, y) && dx * dx + dy * dy > SAFE_RADIUS_TILES * SAFE_RADIUS_TILES) {
                NpcType type = random.nextBoolean() ? NpcType.ZOMBIE : NpcType.ZOMBIE_ALT;
                NpcManager.spawn(type, px(x), px(y), random.nextInt(360));
                placed++;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades de casillas y bordes
    // ---------------------------------------------------------------------------------------------

    private static float px(int tile) {
        return tile * TILE;
    }

    private static boolean inWorld(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE_TILES && y < SIZE_TILES;
    }

    /** Borde norte (o oeste, si {@code vertical}) de la casilla (x, y). Incluye los bordes del final del mapa. */
    private static long edgeKey(int x, int y, boolean vertical) {
        return ((long) x * (SIZE_TILES + 1) + y) * 2 + (vertical ? 1 : 0);
    }

    private static int edgeX(long key) {
        return (int) (key / 2 / (SIZE_TILES + 1));
    }

    private static int edgeY(long key) {
        return (int) (key / 2 % (SIZE_TILES + 1));
    }

    private static boolean isVertical(long key) {
        return key % 2 == 1;
    }

    /** Pone ({@code kind}) o quita ({@code null}) el muro de un borde. */
    private void wall(int x, int y, boolean vertical, StructKind kind) {
        if (x < 0 || y < 0 || x > SIZE_TILES || y > SIZE_TILES) {
            return;
        }
        if (kind == null) {
            walls.remove(edgeKey(x, y, vertical));
        } else {
            walls.put(edgeKey(x, y, vertical), kind);
        }
    }

    /** Línea horizontal en los bordes norte de las casillas x0..x1 de la fila y ({@code null} = quitarla). */
    private void northWalls(int x0, int x1, int y, StructKind kind) {
        for (int x = x0; x <= x1; x++) {
            wall(x, y, false, kind);
        }
    }

    /** Línea vertical en los bordes oeste de las casillas y0..y1 de la columna x ({@code null} = quitarla). */
    private void westWalls(int x, int y0, int y1, StructKind kind) {
        for (int y = y0; y <= y1; y++) {
            wall(x, y, true, kind);
        }
    }

    /** Muros alrededor de las casillas x0..x1, y0..y1. */
    private void room(int x0, int y0, int x1, int y1, StructKind kind) {
        northWalls(x0, x1, y0, kind);
        northWalls(x0, x1, y1 + 1, kind);
        westWalls(x0, y0, y1, kind);
        westWalls(x1 + 1, y0, y1, kind);
    }

    /** Quita al azar algunos tramos de los muros alrededor de las casillas x0..x1, y0..y1. */
    private void ruin(int x0, int y0, int x1, int y1, double chance) {
        for (int x = x0; x <= x1; x++) {
            if (random.nextDouble() < chance) wall(x, y0, false, null);
            if (random.nextDouble() < chance) wall(x, y1 + 1, false, null);
        }
        for (int y = y0; y <= y1; y++) {
            if (random.nextDouble() < chance) wall(x0, y, true, null);
            if (random.nextDouble() < chance) wall(x1 + 1, y, true, null);
        }
    }

    private void ground(int x0, int y0, int x1, int y1, TextureId texture) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (inWorld(x, y)) {
                    ground[x][y] = texture;
                }
            }
        }
    }
}
