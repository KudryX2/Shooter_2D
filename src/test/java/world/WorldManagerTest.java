package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.ItemKind;
import items.ItemManager;
import items.Projectile;
import npc.Npc;
import npc.NpcManager;
import player.Player;
import structs.Struct;
import util.GameClock;

class WorldManagerTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
        NpcManager.init();
        GameClock.reset();
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
    }

    @Test
    void buildGoesToClosestChunk() {
        WorldManager.build(new Struct(600, 10, ItemKind.WALL, TextureId.WALL));
        assertEquals(0, WorldManager.chunks.get(0).walls.size());
        assertEquals(1, WorldManager.chunks.get(1).walls.size());
    }

    @Test
    void wallsAndHalfWallsAreSolidButFloorsAreNot() {
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, TextureId.WALL));
        WorldManager.build(new Struct(400, 100, ItemKind.HALF_WALL, TextureId.HALF_WALL));
        WorldManager.build(new Struct(1000, 1000, ItemKind.FLOOR, TextureId.GRASS));

        assertTrue(WorldManager.isSolidAt(110, 90));
        assertTrue(WorldManager.isSolidAt(400, 100));
        assertFalse(WorldManager.isSolidAt(1000, 1000));
    }

    @Test
    void dropItemOutsideTheMapUsesClosestChunk() {
        WorldManager.dropItem(ItemManager.buildables.getFirst(), -5000, -5000);
        assertEquals(1, WorldManager.chunks.getFirst().droppedItems.size());
    }

    /** Dispara una bala quieta en (x, y) y dice si un muro la ha parado. */
    private static boolean bulletStoppedAt(float x, float y) {
        Player.x = 100_000; // lejos, para que la bala no le dé al jugador
        Player.y = 100_000;
        WorldManager.projectiles.clear();
        WorldManager.projectiles.add(new Projectile(x, y, 0, 0, 600, 1, false));
        WorldManager.update();
        return WorldManager.projectiles.isEmpty();
    }

    @Test
    void bulletsStopAtWallsButCrossHalfWallsAndFloors() {
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, TextureId.WALL));
        WorldManager.build(new Struct(400, 100, ItemKind.HALF_WALL, TextureId.HALF_WALL));
        WorldManager.build(new Struct(700, 100, ItemKind.FLOOR, TextureId.GRASS));

        // Muro horizontal fino: 81 x 16 px (una casilla más el grosor, para cerrar las esquinas)
        assertTrue(bulletStoppedAt(100, 100));
        assertTrue(bulletStoppedAt(140, 107));
        assertFalse(bulletStoppedAt(100, 109));
        assertFalse(bulletStoppedAt(141, 100));
        assertFalse(bulletStoppedAt(400, 100));
        assertFalse(bulletStoppedAt(700, 100));
    }

    /** Dispara una bala hacia +x desde (x, y) y avanza un frame en el que recorre {@code step} píxeles. */
    private static Projectile fireRight(float x, float y, float step) {
        Player.x = 100_000;
        Player.y = 100_000;
        WorldManager.projectiles.clear();
        Projectile p = new Projectile(x, y, 0, 1, 600, 50, false); // rotación 0 = hacia +x, 1 px/ms
        WorldManager.projectiles.add(p);
        GameClock.tick(step);
        WorldManager.update();
        return p;
    }

    @Test
    void fastBulletsCannotJumpOverWalls() {
        // Muro vertical de 16 px cruzado en el camino de una bala que avanza 300 px en un frame
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, TextureId.WALL, true));
        fireRight(0, 100, 300); // empieza antes del muro y acabaría muy detrás
        assertTrue(WorldManager.projectiles.isEmpty());
    }

    @Test
    void fastBulletsHitTheNpcTheyFlyThrough() {
        Npc target = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 100, 100, 0);
        fireRight(0, 100, 300);
        assertTrue(WorldManager.projectiles.isEmpty());
        assertTrue(target.hp < 100);
    }

    @Test
    void wallsProtectNpcsBehindThem() {
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, TextureId.WALL));
        Npc behind = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 200, 100, 0);
        fireRight(0, 100, 300); // el tramo cruza el muro y luego el NPC: gana el muro
        assertTrue(WorldManager.projectiles.isEmpty());
        assertEquals(100, behind.hp);
    }

    @Test
    void bulletsHitTheFirstNpcInTheirPath() {
        Npc near = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 100, 100, 0);
        Npc far = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 200, 100, 0);
        fireRight(0, 100, 300);
        assertTrue(near.hp < 100);
        assertEquals(100, far.hp);
    }

    @Test
    void bulletsMissWallsBesideTheirPath() {
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, TextureId.WALL));
        fireRight(0, 110, 300); // pasa 10 px por debajo de un muro horizontal de 16 px de grosor
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void bulletGridMatchesCheckingEveryWall() {
        // Muros fuera de la cuadrícula y en coordenadas negativas, como los que construye el jugador
        var random = new java.util.Random(7);
        var walls = new java.util.ArrayList<Struct>();
        for (int i = 0; i < 200; i++) {
            Struct wall = new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400,
                    ItemKind.WALL, TextureId.WALL, random.nextBoolean());
            walls.add(wall);
            WorldManager.build(wall);
        }
        for (int i = 0; i < 2_000; i++) {
            float x = random.nextFloat() * 1600 - 500;
            float y = random.nextFloat() * 1600 - 500;
            boolean expected = walls.stream().anyMatch(w -> w.overlaps(x, y, 0, 0));
            assertEquals(expected, bulletStoppedAt(x, y), "en " + x + "," + y);
        }
    }

    @Test
    void isSolidAtMatchesCheckingEveryWallAndHalfWall() {
        var random = new java.util.Random(11);
        var solids = new java.util.ArrayList<Struct>();
        for (int i = 0; i < 200; i++) {
            ItemKind kind = random.nextBoolean() ? ItemKind.WALL : ItemKind.HALF_WALL;
            Struct s = new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400, kind,
                    TextureId.WALL, random.nextBoolean());
            solids.add(s);
            WorldManager.build(s);
            WorldManager.build(new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400,
                    ItemKind.FLOOR, TextureId.GRASS)); // los suelos nunca bloquean
        }
        for (int i = 0; i < 5_000; i++) {
            float x = random.nextFloat() * 1600 - 500;
            float y = random.nextFloat() * 1600 - 500;
            boolean expected = solids.stream().anyMatch(
                    s -> s.overlaps(x, y, WorldManager.CHARACTER_HALF, WorldManager.CHARACTER_HALF));
            assertEquals(expected, WorldManager.isSolidAt(x, y), "en " + x + "," + y);
        }
    }

    @Test
    void hasWallAtOnlyMatchesTheSameEdge() {
        WorldManager.build(Struct.northWall(2, 2, ItemKind.WALL, TextureId.WALL));
        assertTrue(WorldManager.hasWallAt(Struct.northWall(2, 2, ItemKind.HALF_WALL, TextureId.HALF_WALL)));
        assertFalse(WorldManager.hasWallAt(Struct.westWall(2, 2, ItemKind.WALL, TextureId.WALL)));  // esquina
        assertFalse(WorldManager.hasWallAt(Struct.northWall(3, 2, ItemKind.WALL, TextureId.WALL))); // al lado
    }

    @Test
    void resetForgetsOldWalls() {
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, TextureId.WALL));
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        assertFalse(bulletStoppedAt(100, 100));
        assertFalse(WorldManager.isSolidAt(100, 100));
    }

    @Test
    void gunsCannotBeBuilt() {
        assertThrows(IllegalArgumentException.class, () -> new Struct(0, 0, ItemKind.GUN, TextureId.WALL));
    }
}
