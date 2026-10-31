package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Geometry.distance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.GunType;
import items.MeleeType;
import items.MeleeWeapon;
import npc.Npc;
import npc.NpcKind;
import player.Player;
import util.GameClock;
import world.Chunk;
import world.Structures;
import world.WorldManager;

class OutpostWorldTest {
    /** Del centro de la casa inicial (11x11 casillas de 64 px) a sus esquinas. */
    private static final float HOUSE_RADIUS = 500;

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.activeChunks.clear();
        WorldManager.projectiles.clear();
        WorldManager.npcs.clear();
        Player.inventory.removeAll();
        OutpostWorld.create();
        Player.x = Player.spawnX();
        Player.y = Player.spawnY();
        Player.hp = 100;
        GameClock.reset();
        GameClock.tick(8);
    }

    @Test
    void playerStartsFreeWithAPistolAndGunsInTheHouse() {
        assertFalse(Structures.isSolidAt(Player.spawnX(), Player.spawnY()));
        assertTrue(Player.inventory.get(0) instanceof Gun);

        long gunsInHouse = WorldManager.chunks.stream()
                .flatMap(chunk -> chunk.droppedItems.stream())
                .filter(d -> d.item() instanceof Gun)
                .filter(d -> distance(d.x(), d.y(), Player.spawnX(), Player.spawnY()) < HOUSE_RADIUS)
                .count();
        assertTrue(gunsInHouse >= 2, "la casa inicial debería tener armas");

        // Una de cada arma, para probarlas sin salir de casa
        Set<Object> weaponTypesInHouse = WorldManager.chunks.stream()
                .flatMap(chunk -> chunk.droppedItems.stream())
                .filter(d -> distance(d.x(), d.y(), Player.spawnX(), Player.spawnY()) < HOUSE_RADIUS)
                .map(d -> switch (d.item()) {
                    case Gun gun -> gun.type;
                    case MeleeWeapon melee -> melee.type;
                    default -> "otro";
                })
                .collect(Collectors.toSet());
        assertTrue(weaponTypesInHouse.containsAll(List.of(GunType.values())), "faltan armas de fuego en la casa");
        assertTrue(weaponTypesInHouse.containsAll(List.of(MeleeType.values())), "faltan armas cuerpo a cuerpo");

        long medkitsInHouse = WorldManager.chunks.stream()
                .flatMap(chunk -> chunk.droppedItems.stream())
                .filter(d -> d.item() instanceof Consumable)
                .filter(d -> distance(d.x(), d.y(), Player.spawnX(), Player.spawnY()) < HOUSE_RADIUS)
                .count();
        assertTrue(medkitsInHouse >= 1, "la casa inicial debería tener botiquines");
    }

    @Test
    void npcsSpawnOnFreeGroundAndNoZombiesNearSpawn() {
        assertTrue(WorldManager.npcs.size() > 50);
        for (Npc npc : WorldManager.npcs) {
            assertFalse(Structures.isSolidAt(npc.x, npc.y), "NPC dentro de un muro en " + npc.x + "," + npc.y);
            if (npc.kind == NpcKind.ZOMBIE) {
                assertTrue(distance(npc.x, npc.y, Player.spawnX(), Player.spawnY()) > 1000, "zombi demasiado cerca");
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
                assertFalse(Structures.isSolidAt(d.x(), d.y()), d.item().name + " dentro de un muro");
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
