package player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.Gun;
import items.GunType;
import items.StructItem;
import structs.Struct;
import structs.StructKind;
import world.Chunk;
import world.Structures;
import world.WorldManager;

class BuildToolTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
        Player.inventory.removeAll();
        Player.inventory.select(0);
        Player.x = 0;
        Player.y = 0;
    }

    @Test
    void wallsCanOnlyBeBuiltOnFreeEdgesInRangeAndNotOnThePlayer() {
        Player.y = -30; // casi sobre el borde norte de la casilla (0, 0), en y = -32.5
        Structures.build(Struct.northWall(2, 2, StructKind.WALL));

        assertFalse(BuildTool.canBuild(Struct.northWall(2, 2, StructKind.HALF_WALL)), "ocupado");
        assertTrue(BuildTool.canBuild(Struct.westWall(2, 2, StructKind.WALL)), "esquina con el otro");
        assertTrue(BuildTool.canBuild(Struct.northWall(3, 2, StructKind.WALL)), "a continuación");
        assertFalse(BuildTool.canBuild(Struct.northWall(0, 0, StructKind.WALL)), "encima del jugador");
        assertFalse(BuildTool.canBuild(Struct.northWall(10, 0, StructKind.WALL)), "fuera de alcance");
        assertTrue(BuildTool.canBuild(new Struct(0, 0, StructKind.FLOOR, TextureId.GRASS)), "suelo bajo el jugador");
    }

    @Test
    void leftClickWithAWallInHandBuildsThePreview() {
        Player.inventory.add(new StructItem(StructKind.WALL));
        Player.aimAt(10, 40); // cerca del borde horizontal y = 32.5
        Player.updateBuildPreview();
        assertEquals(Struct.northWall(0, 1, StructKind.WALL), BuildTool.preview());
        assertTrue(BuildTool.canBuildPreview());

        Player.primaryAction(true);
        assertTrue(Structures.hasWallAt(BuildTool.preview()));
        assertFalse(BuildTool.canBuildPreview(), "ese borde ya está ocupado");
    }

    @Test
    void noPreviewWithoutABuildableInHand() {
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.aimAt(10, 60);
        Player.updateBuildPreview();
        assertNull(BuildTool.preview());
    }

    @Test
    void rightClickWithAWallInHandDemolishesTheAimedWall() {
        Struct wall = Struct.northWall(1, 1, StructKind.WALL);
        Structures.build(wall);
        Player.inventory.add(new StructItem(StructKind.WALL));
        Player.aimAt(wall.x(), wall.y() + 3);
        Player.secondaryAction();
        assertFalse(Structures.hasWallAt(wall));
    }

    @Test
    void rightClickWithAGunDoesNotDemolish() {
        Struct wall = Struct.northWall(1, 1, StructKind.WALL);
        Structures.build(wall);
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.aimAt(wall.x(), wall.y());
        Player.secondaryAction();
        assertTrue(Structures.hasWallAt(wall));
    }

    @Test
    void demolishRemovesTheAimedWallWithinRange() {
        Struct wall = Struct.northWall(2, 2, StructKind.WALL);
        Struct next = Struct.northWall(3, 2, StructKind.HALF_WALL);
        Struct far = Struct.westWall(12, 0, StructKind.WALL);
        Structures.build(wall);
        Structures.build(next);
        Structures.build(far);

        assertTrue(BuildTool.demolish(wall.x() + 5, wall.y() - 3)); // apuntando cerca del muro
        assertFalse(Structures.hasWallAt(wall));
        assertFalse(Structures.isBlocked(wall.x(), wall.y(), 1, 1), "ya no bloquea el paso");
        assertTrue(Structures.hasWallAt(next), "el de al lado sigue en pie");
        assertTrue(WorldManager.chunks.stream().noneMatch(c -> c.walls.contains(wall)), "ya no se dibuja");

        assertFalse(BuildTool.demolish(far.x(), far.y()), "fuera de alcance");
        assertTrue(Structures.hasWallAt(far));

        assertTrue(BuildTool.demolish(next.x(), next.y()), "también medios muros");
        assertFalse(Structures.hasWallAt(next));
    }

    @Test
    void demolishOnlyRemovesBuiltFloorsAndNeverTheMapGround() {
        Structures.build(new Struct(65, 0, StructKind.FLOOR, TextureId.GRASS)); // suelo del mapa
        Structures.build(new Struct(65, 0, StructKind.FLOOR, TextureId.DIRT));  // construido encima

        assertTrue(BuildTool.demolish(70, 5));
        assertEquals(1, WorldManager.chunks.getFirst().floors.size());
        assertEquals(TextureId.GRASS, WorldManager.chunks.getFirst().floors.getFirst().texture());
        assertFalse(BuildTool.demolish(70, 5), "el suelo del mapa no se quita");
    }
}
