package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Geometry.distance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import npc.Npc;
import npc.NpcKind;
import player.Player;
import util.GameClock;
import world.Chunk;
import world.WorldManager;

class OutpostWorldTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.activeChunks.clear();
        WorldManager.projectiles.clear();
        WorldManager.npcs.clear();
        Player.inventory.clear();
        OutpostWorld.create();
        Player.x = Player.SPAWN_X;
        Player.y = Player.SPAWN_Y;
        Player.hp = 100;
        GameClock.reset();
        GameClock.tick(8);
    }

    @Test
    void playerStartsFreeWithAPistolAndGunsInTheHouse() {
        assertFalse(WorldManager.isSolidAt(Player.SPAWN_X, Player.SPAWN_Y));
        assertTrue(Player.inventory.getFirst() instanceof Gun);

        long gunsInHouse = WorldManager.chunks.stream()
                .flatMap(chunk -> chunk.droppedItems.stream())
                .filter(d -> d.item() instanceof Gun)
                .filter(d -> distance(d.x(), d.y(), Player.SPAWN_X, Player.SPAWN_Y) < 300)
                .count();
        assertTrue(gunsInHouse >= 2, "la casa inicial debería tener armas");

        long medkitsInHouse = WorldManager.chunks.stream()
                .flatMap(chunk -> chunk.droppedItems.stream())
                .filter(d -> d.item() instanceof Consumable)
                .filter(d -> distance(d.x(), d.y(), Player.SPAWN_X, Player.SPAWN_Y) < 300)
                .count();
        assertTrue(medkitsInHouse >= 1, "la casa inicial debería tener botiquines");
    }

    @Test
    void npcsSpawnOnFreeGroundAndNoZombiesNearSpawn() {
        assertTrue(WorldManager.npcs.size() > 50);
        for (Npc npc : WorldManager.npcs) {
            assertFalse(WorldManager.isSolidAt(npc.x, npc.y), "NPC dentro de un muro en " + npc.x + "," + npc.y);
            if (npc.kind == NpcKind.ZOMBIE) {
                assertTrue(distance(npc.x, npc.y, Player.SPAWN_X, Player.SPAWN_Y) > 1000, "zombi demasiado cerca");
            }
        }
    }

    @Test
    void humansLookHumanUntilTheyTurn() {
        Npc human = WorldManager.npcs.stream().filter(n -> n.kind == NpcKind.HUMAN).findFirst().orElseThrow();
        assertEquals(TextureId.HUMAN, human.texture);
        for (Npc npc : WorldManager.npcs) {
            assertEquals(npc.kind == NpcKind.HUMAN, npc.texture == TextureId.HUMAN);
        }

        human.die(false); // sin headshot se convierte en zombi, pero conserva su aspecto hasta levantarse
        assertEquals(NpcKind.ZOMBIE, human.kind);
        assertTrue(human.isRising());
        assertEquals(TextureId.HUMAN, human.texture);
    }

    @Test
    void droppedItemsAreNotInsideWalls() {
        for (Chunk chunk : WorldManager.chunks) {
            for (DroppedItem d : chunk.droppedItems) {
                assertFalse(WorldManager.isSolidAt(d.x(), d.y()), d.item().name + " dentro de un muro");
            }
        }
    }

    @Test
    void simulationStaysFastWithWaves() {
        OutpostWorld.spawnWave();
        long worst = 0;
        for (int i = 0; i < 1_500; i++) {
            GameClock.tick(8);
            long start = System.nanoTime();
            WorldManager.update();
            worst = Math.max(worst, System.nanoTime() - start);
            assertTrue(WorldManager.projectiles.size() < 2_000, "proyectiles sin control");
        }
        long worstMs = worst / 1_000_000;
        System.out.println("Puesto avanzado: peor frame " + worstMs + " ms, NPCs " + WorldManager.npcs.size());
        assertTrue(worstMs < 200, "frame demasiado lento: " + worstMs + " ms");
    }
}
