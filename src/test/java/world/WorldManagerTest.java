package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.Projectile;
import items.StructItem;
import player.Player;
import structs.Struct;
import structs.StructKind;
import util.GameClock;

class WorldManagerTest {

    @BeforeEach
    void setUp() {
        GameClock.reset();
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
    }

    @Test
    void dropItemOutsideTheMapUsesClosestChunk() {
        WorldManager.dropItem(new StructItem(StructKind.WALL), -5000, -5000);
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
    void resetForgetsOldWalls() {
        Structures.build(new Struct(100, 100, StructKind.WALL, TextureId.WALL));
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        assertFalse(bulletStoppedAt(100, 100));
        assertFalse(Structures.isSolidAt(100, 100));
    }
}
