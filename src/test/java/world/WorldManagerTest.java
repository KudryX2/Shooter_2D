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
import player.Player;
import structs.Struct;

class WorldManagerTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
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

        assertTrue(bulletStoppedAt(100, 100));
        assertTrue(bulletStoppedAt(129, 71)); // borde de la caja de impacto (30 px)
        assertFalse(bulletStoppedAt(131, 100));
        assertFalse(bulletStoppedAt(400, 100));
        assertFalse(bulletStoppedAt(700, 100));
    }

    @Test
    void bulletGridMatchesCheckingEveryWall() {
        // Muros fuera de la cuadrícula y en coordenadas negativas, como los que construye el jugador
        var random = new java.util.Random(7);
        var walls = new java.util.ArrayList<Struct>();
        for (int i = 0; i < 200; i++) {
            Struct wall = new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400, ItemKind.WALL, TextureId.WALL);
            walls.add(wall);
            WorldManager.build(wall);
        }
        for (int i = 0; i < 2_000; i++) {
            float x = random.nextFloat() * 1600 - 500;
            float y = random.nextFloat() * 1600 - 500;
            boolean expected = walls.stream().anyMatch(w -> util.Geometry.overlaps(x, y, w.x(), w.y(), 30));
            assertEquals(expected, bulletStoppedAt(x, y), "en " + x + "," + y);
        }
    }

    @Test
    void isSolidAtMatchesCheckingEveryWallAndHalfWall() {
        var random = new java.util.Random(11);
        var solids = new java.util.ArrayList<Struct>();
        for (int i = 0; i < 200; i++) {
            ItemKind kind = random.nextBoolean() ? ItemKind.WALL : ItemKind.HALF_WALL;
            Struct s = new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400, kind, TextureId.WALL);
            solids.add(s);
            WorldManager.build(s);
            WorldManager.build(new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400,
                    ItemKind.FLOOR, TextureId.GRASS)); // los suelos nunca bloquean
        }
        for (int i = 0; i < 5_000; i++) {
            float x = random.nextFloat() * 1600 - 500;
            float y = random.nextFloat() * 1600 - 500;
            boolean expected = solids.stream().anyMatch(s -> util.Geometry.overlaps(x, y, s.x(), s.y(), Struct.SIZE));
            assertEquals(expected, WorldManager.isSolidAt(x, y), "en " + x + "," + y);
        }
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
