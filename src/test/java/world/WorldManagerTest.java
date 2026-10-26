package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.ItemKind;
import items.ItemManager;
import structs.Struct;

class WorldManagerTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
        WorldManager.chunks.clear();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
    }

    @Test
    void buildGoesToClosestChunk() {
        WorldManager.build(new Struct(600, 10, ItemKind.WALL, 2));
        assertEquals(0, WorldManager.chunks.get(0).walls.size());
        assertEquals(1, WorldManager.chunks.get(1).walls.size());
    }

    @Test
    void wallsAndHalfWallsAreSolidButFloorsAreNot() {
        WorldManager.build(new Struct(100, 100, ItemKind.WALL, 2));
        WorldManager.build(new Struct(400, 100, ItemKind.HALF_WALL, 4));
        WorldManager.build(new Struct(1000, 1000, ItemKind.FLOOR, 3));

        assertTrue(WorldManager.isSolidAt(110, 90));
        assertTrue(WorldManager.isSolidAt(400, 100));
        assertFalse(WorldManager.isSolidAt(1000, 1000));
    }

    @Test
    void dropItemOutsideTheMapUsesClosestChunk() {
        WorldManager.dropItem(ItemManager.buildables.getFirst(), -5000, -5000);
        assertEquals(1, WorldManager.chunks.getFirst().droppedItems.size());
    }

    @Test
    void gunsCannotBeBuilt() {
        assertThrows(IllegalArgumentException.class, () -> new Struct(0, 0, ItemKind.GUN, 0));
    }
}
