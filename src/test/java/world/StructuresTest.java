package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import npc.Npc;
import structs.Struct;
import structs.StructKind;
import util.GameClock;

class StructuresTest {

    @BeforeEach
    void setUp() {
        GameClock.reset();
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
    }

    @Test
    void buildGoesToClosestChunk() {
        Structures.build(new Struct(600, 10, StructKind.WALL, TextureId.WALL));
        assertEquals(0, WorldManager.chunks.get(0).walls.size());
        assertEquals(1, WorldManager.chunks.get(1).walls.size());
    }

    @Test
    void wallsAndHalfWallsAreSolidButFloorsAreNot() {
        Structures.build(new Struct(100, 100, StructKind.WALL, TextureId.WALL));
        Structures.build(new Struct(400, 100, StructKind.HALF_WALL, TextureId.HALF_WALL));
        Structures.build(new Struct(1000, 1000, StructKind.FLOOR, TextureId.GRASS));

        assertTrue(Structures.isSolidAt(110, 90));
        assertTrue(Structures.isSolidAt(400, 100));
        assertFalse(Structures.isSolidAt(1000, 1000));
    }

    @Test
    void isSolidAtMatchesCheckingEveryWallAndHalfWall() {
        var random = new java.util.Random(11);
        var solids = new java.util.ArrayList<Struct>();
        for (int i = 0; i < 200; i++) {
            StructKind kind = random.nextBoolean() ? StructKind.WALL : StructKind.HALF_WALL;
            Struct s = new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400, kind,
                    TextureId.WALL, random.nextBoolean());
            solids.add(s);
            Structures.build(s);
            Structures.build(new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400,
                    StructKind.FLOOR, TextureId.GRASS)); // los suelos nunca bloquean
        }
        for (int i = 0; i < 5_000; i++) {
            float x = random.nextFloat() * 1600 - 500;
            float y = random.nextFloat() * 1600 - 500;
            boolean expected = solids.stream().anyMatch(
                    s -> s.overlaps(x, y, Npc.BODY_HALF, Npc.BODY_HALF));
            assertEquals(expected, Structures.isSolidAt(x, y), "en " + x + "," + y);
        }
    }

    @Test
    void hasWallAtOnlyMatchesTheSameEdge() {
        Structures.build(Struct.northWall(2, 2, StructKind.WALL));
        assertTrue(Structures.hasWallAt(Struct.northWall(2, 2, StructKind.HALF_WALL)));
        assertFalse(Structures.hasWallAt(Struct.westWall(2, 2, StructKind.WALL)));  // esquina
        assertFalse(Structures.hasWallAt(Struct.northWall(3, 2, StructKind.WALL))); // al lado
    }
}
