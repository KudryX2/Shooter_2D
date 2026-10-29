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
 * Mundo "Puesto avanzado". La casa inicial está en el centro del mapa y el resto, alrededor:
 *
 * <pre>
 *              cementerio
 *                  |
 *   ruinas       casa ------ aldea
 *                  |
 *                fuerte
 * </pre>
 *
 * <ul>
 *   <li>Casa inicial (donde aparece el jugador) con armas y materiales, ventanas y un anillo de sacos terreros
 *       vigilado por humanos aliados.</li>
 *   <li>Caminos de tierra hacia el norte (cementerio), el este (aldea) y el sur (fuerte).</li>
 *   <li>Aldea en ruinas con civiles que huyen de los zombis y algo de botín.</li>
 *   <li>Fuerte militar con humanos agresivos y una armería con AK-47 y sniper.</li>
 *   <li>Cementerio infestado de zombis con un mausoleo.</li>
 *   <li>Ruinas al oeste.</li>
 * </ul>
 *
 * <p>Todo se diseña en casillas ({@link Struct#SIZE} píxeles) <b>relativas a la casa</b>: la (0, 0) es la casilla
 * donde aparece el jugador, en el centro del mapa, y el mapa va de -{@link #HALF_TILES} a {@link #HALF_TILES} - 1 en
 * cada eje. Los muros van sobre los bordes de las casillas: cada casilla tiene un borde norte y uno oeste. Los
 * edificios se definen por sus casillas interiores. El mapa usa una semilla fija, así que siempre sale igual.
 */
final class OutpostWorld {
    private static final int TILE = (int) Struct.SIZE;
    private static final int SIZE_TILES = 160;
    /** La casa: casilla (HALF_TILES, HALF_TILES) del mapa, o (0, 0) en las coordenadas de este diseño. */
    private static final int HALF_TILES = SIZE_TILES / 2;
    private static final int CHUNK_TILES = 10;
    private static final long SEED = 2026;

    /** Ningún zombi aparece al crear el mundo a menos de esta distancia (en casillas) del jugador. */
    private static final int SAFE_RADIUS_TILES = 20;
    /** Zombis sueltos repartidos por todo el mapa (aparte de los de cada zona). */
    private static final int LOOSE_ZOMBIES = 50;
    /** Por debajo de 600 px (rango de persecución) para que la oleada vaya a por el jugador directamente. */
    private static final float WAVE_RADIUS = 550;
    private static final int WAVE_SIZE = 16;

    private final Random random = new Random(SEED);
    /** Por casilla del mapa (no relativa a la casa). */
    private final TextureId[][] ground = new TextureId[SIZE_TILES][SIZE_TILES];
    /** Muros por borde (ver {@link #edgeKey}). */
    private final Map<Long, StructKind> walls = new HashMap<>();

    private OutpostWorld() {
    }

    static void create() {
        Player.setSpawn(px(0), px(0));
        Player.teleportToSpawn();
        Player.inventory.add(new Gun(GunType.PISTOL));

        for (int i = 0; i < SIZE_TILES; i += CHUNK_TILES) {
            for (int j = 0; j < SIZE_TILES; j += CHUNK_TILES) {
                WorldManager.chunks.add(new Chunk((i - HALF_TILES) * TILE, (j - HALF_TILES) * TILE));
            }
        }

        OutpostWorld world = new OutpostWorld();
        world.layout();
        world.build();
        world.populate();
        System.out.println("Puesto avanzado: " + WorldManager.chunks.size() + " chunks, "
                + world.walls.size() + " muros, " + WorldManager.npcs.size() + " NPCs");
    }

    /** Oleada de zombis en círculo alrededor del jugador. Tecla H. */
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
    }

    private void roads() {
        // Sur: de la puerta de la casa hasta la altura del fuerte y luego al este hasta su puerta
        ground(-1, 6, 1, 41, TextureId.DIRT);
        ground(-1, 39, 16, 41, TextureId.DIRT);
        // Este: atraviesa la aldea
        ground(6, -1, 40, 1, TextureId.DIRT);
        // Norte: de la salida norte de los sacos terreros a la entrada del cementerio
        ground(-1, -40, 1, -8, TextureId.DIRT);
    }

    /** Casa inicial, en la casilla (0, 0): donde aparece el jugador. */
    private void house() {
        // Anillo de sacos terreros (medio muro: frena a los zombis pero deja pasar las balas)
        room(-7, -7, 7, 7, StructKind.HALF_WALL);
        northWalls(-1, 1, 8, null);  // salida sur
        westWalls(8, -1, 1, null);   // salida este
        northWalls(-1, 1, -7, null); // salida norte, hacia el cementerio

        // Casa
        room(-4, -4, 4, 4, StructKind.WALL);
        ground(-4, -4, 4, 4, TextureId.DIRT);
        northWalls(-1, 1, 5, null); // puerta
        // Ventanas para disparar desde dentro
        northWalls(-1, 1, -4, StructKind.HALF_WALL);
        westWalls(-4, -1, 1, StructKind.HALF_WALL);
        westWalls(5, -1, 1, StructKind.HALF_WALL);
        // Banco de trabajo (rincón de los materiales)
        northWalls(2, 3, 2, StructKind.HALF_WALL);
    }

    /** Al este, a ambos lados del camino. */
    private void village() {
        hut(24, -7, 28, -4, 'S');
        hut(34, -7, 38, -4, 'S');
        hut(27, 4, 33, 7, 'N');
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

    /** Al sur, con la puerta al oeste donde acaba el camino. */
    private void fort() {
        int x0 = 17, y0 = 32, x1 = 35, y1 = 48;
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

        westWalls(x0, 39, 41, null); // puerta oeste, al final del camino

        // Barracón / armería
        room(28, 35, 33, 39, StructKind.WALL);
        westWalls(28, 36, 38, null);

        // Coberturas en el patio
        northWalls(20, 23, 36, StructKind.HALF_WALL);
        northWalls(20, 23, 44, StructKind.HALF_WALL);
        northWalls(29, 32, 44, StructKind.HALF_WALL);
    }

    /** Al norte, con la entrada al sur, donde acaba el camino. */
    private void cemetery() {
        int x0 = -9, y0 = -59, x1 = 9, y1 = -41;
        room(x0, y0, x1, y1, StructKind.HALF_WALL);
        ground(x0, y0, x1, y1, TextureId.DIRT);
        northWalls(-1, 1, y1 + 1, null); // entrada sur

        // Mausoleo, con la puerta mirando a la entrada
        room(-2, -52, 2, -48, StructKind.WALL);
        northWalls(-1, 1, -47, null);

        // Tumbas en filas, dejando libre el pasillo central y el mausoleo
        for (int x = x0 + 2; x <= x1; x += 3) {
            for (int y = y0 + 2; y <= y1; y += 3) {
                boolean aisle = x >= -2 && x <= 2;
                boolean nearMausoleum = x >= -4 && x <= 4 && y >= -54 && y <= -46;
                if (!aisle && !nearMausoleum) {
                    northWalls(x, x, y, StructKind.HALF_WALL);
                }
            }
        }
    }

    /** Restos de muros sueltos (rectos o en L) al oeste. */
    private void ruins() {
        for (int i = 0; i < 20; i++) {
            int x = -70 + random.nextInt(31);
            int y = -18 + random.nextInt(36);
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

    // ---------------------------------------------------------------------------------------------
    // Construcción y población
    // ---------------------------------------------------------------------------------------------

    private void build() {
        for (int x = 0; x < SIZE_TILES; x++) {
            for (int y = 0; y < SIZE_TILES; y++) {
                float worldX = px(x - HALF_TILES), worldY = px(y - HALF_TILES);
                Structures.build(new Struct(worldX, worldY, StructKind.FLOOR, ground[x][y]));
            }
        }
        walls.forEach((key, kind) -> {
            int x = edgeX(key) - HALF_TILES, y = edgeY(key) - HALF_TILES;
            Structures.build(isVertical(key)
                    ? Struct.westWall(x, y, kind)
                    : Struct.northWall(x, y, kind));
        });
    }

    private void populate() {
        // Casa inicial: armero al norte y materiales junto al banco de trabajo
        item(new Gun(GunType.SHOTGUN), -3, -3);
        item(new Gun(GunType.SNIPER), 3, -3);
        item(new StructItem(StructKind.WALL), 2, 3);
        item(new StructItem(StructKind.HALF_WALL), 3, 3);
        item(new StructItem(StructKind.FLOOR), 4, 3);
        item(Consumable.medkit(), -3, 3);
        item(Consumable.medkit(), -2, 3);
        // Guardias aliados entre la casa y los sacos terreros (solo disparan a zombis)
        human(NpcType.GUARD, -3, 7);
        human(NpcType.GUARD, 3, 7);
        human(NpcType.GUARD, 7, -3);
        human(NpcType.GUARD, 7, 3);

        // Aldea: civiles que huyen, un guardia en el camino y botín aleatorio
        item(new Gun(GunType.random()), 26, -6);
        item(new Gun(GunType.random()), 36, -6);
        item(new Gun(GunType.random()), 30, 6);
        item(new StructItem(StructKind.HALF_WALL), 38, -7);
        item(Consumable.medkit(), 24, -4);
        item(Consumable.medkit(), 32, 5);
        human(NpcType.CIVILIAN, 24, -6);
        human(NpcType.CIVILIAN, 28, -5);
        human(NpcType.CIVILIAN, 34, -5);
        human(NpcType.CIVILIAN, 38, -5);
        human(NpcType.CIVILIAN, 28, 5);
        human(NpcType.CIVILIAN, 32, 6);
        human(NpcType.GUARD, 31, 0);
        zombies(6, 51, -9, 61, 11);

        // Fuerte: soldados agresivos (también disparan al jugador) y la armería
        item(new Gun(GunType.ASSAULT_RIFLE), 30, 36);
        item(new Gun(GunType.ASSAULT_RIFLE), 32, 36);
        item(new Gun(GunType.SNIPER), 31, 38);
        item(new StructItem(StructKind.WALL), 33, 38);
        item(Consumable.medkit(), 29, 38);
        item(Consumable.medkit(), 33, 35);
        human(NpcType.SOLDIER, 21, 34);
        human(NpcType.SOLDIER, 25, 40);
        human(NpcType.SOLDIER, 19, 40);
        human(NpcType.SOLDIER, 21, 46);
        human(NpcType.SOLDIER, 31, 46);
        zombies(10, 16, 61, 36, 69);

        // Cementerio: la horda, y un premio dentro del mausoleo
        item(new Gun(GunType.SNIPER), 0, -50);
        item(new Gun(GunType.random()), -1, -51);
        item(Consumable.medkit(), 1, -51);
        item(Consumable.medkit(), 1, -49);
        zombies(25, -9, -59, 9, -41);

        // Ruinas
        item(new Gun(GunType.random()), -65, -3);
        item(new Gun(GunType.random()), -45, 12);
        zombies(15, -70, -18, -35, 22);

        // Zombis sueltos por todo el mapa
        zombies(LOOSE_ZOMBIES, -HALF_TILES, -HALF_TILES, HALF_TILES - 1, HALF_TILES - 1);
    }

    private void item(Item item, int x, int y) {
        WorldManager.dropItem(item, px(x), px(y));
    }

    private void human(NpcType type, int x, int y) {
        if (inWorld(x, y)) {
            NpcManager.spawn(type, px(x), px(y), random.nextInt(360));
        }
    }

    /** Hasta {@code count} zombis en casillas del rectángulo, lejos de la casa (donde aparece el jugador). */
    private void zombies(int count, int x0, int y0, int x1, int y1) {
        int placed = 0;
        for (int attempt = 0; attempt < count * 20 && placed < count; attempt++) {
            int x = x0 + random.nextInt(x1 - x0 + 1);
            int y = y0 + random.nextInt(y1 - y0 + 1);
            if (inWorld(x, y) && x * x + y * y > SAFE_RADIUS_TILES * SAFE_RADIUS_TILES) {
                NpcType type = random.nextBoolean() ? NpcType.ZOMBIE : NpcType.ZOMBIE_ALT;
                NpcManager.spawn(type, px(x), px(y), random.nextInt(360));
                placed++;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades de casillas y bordes (x, y relativas a la casa)
    // ---------------------------------------------------------------------------------------------

    /** Posición en el mundo del centro de la casilla (relativa a la casa) {@code tile}. */
    private static float px(int tile) {
        return tile * TILE;
    }

    private static boolean inWorld(int x, int y) {
        return x >= -HALF_TILES && y >= -HALF_TILES && x < HALF_TILES && y < HALF_TILES;
    }

    /**
     * Borde norte (o oeste, si {@code vertical}) de la casilla (x, y). Incluye los bordes del final del mapa. Se
     * guarda con coordenadas del mapa (de 0 a {@link #SIZE_TILES}) para que la clave no sea negativa.
     */
    private static long edgeKey(int x, int y, boolean vertical) {
        long mapX = x + HALF_TILES, mapY = y + HALF_TILES;
        return (mapX * (SIZE_TILES + 1) + mapY) * 2 + (vertical ? 1 : 0);
    }

    /** Columna en coordenadas del mapa (de 0 a {@link #SIZE_TILES}). */
    private static int edgeX(long key) {
        return (int) (key / 2 / (SIZE_TILES + 1));
    }

    /** Fila en coordenadas del mapa (de 0 a {@link #SIZE_TILES}). */
    private static int edgeY(long key) {
        return (int) (key / 2 % (SIZE_TILES + 1));
    }

    private static boolean isVertical(long key) {
        return key % 2 == 1;
    }

    /** Pone ({@code kind}) o quita ({@code null}) el muro de un borde. */
    private void wall(int x, int y, boolean vertical, StructKind kind) {
        if (x < -HALF_TILES || y < -HALF_TILES || x > HALF_TILES || y > HALF_TILES) {
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
                    ground[x + HALF_TILES][y + HALF_TILES] = texture;
                }
            }
        }
    }
}
