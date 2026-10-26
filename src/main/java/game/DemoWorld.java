package game;

import java.util.concurrent.ThreadLocalRandom;

import items.ItemKind;
import items.ItemManager;
import npc.NpcManager;
import player.Player;
import structs.Struct;
import world.Chunk;
import world.WorldManager;

/** Mapa de pruebas. */
final class DemoWorld {
    private static final int WORLD_SIZE = 10_000;
    private static final int CHUNK_SIZE = 650;
    private static final int TILE = (int) Struct.SIZE;

    private DemoWorld() {
    }

    static void create() {
        Player.inventory.add(ItemManager.guns.getFirst().copy());

        for (int i = 0; i < WORLD_SIZE; i += CHUNK_SIZE) {
            for (int j = 0; j < WORLD_SIZE; j += CHUNK_SIZE) {
                WorldManager.chunks.add(new Chunk(i, j));
            }
        }
        System.out.println("Se han creado " + WorldManager.chunks.size() + " chunks");

        // Objetos en el suelo
        WorldManager.dropItem(ItemManager.buildables.get(0), -100, 500);
        WorldManager.dropItem(ItemManager.buildables.get(1), -100, 550);
        WorldManager.dropItem(ItemManager.buildables.get(2), -100, 600);
        WorldManager.dropItem(ItemManager.guns.get(1).copy(), 100, 900);
        WorldManager.dropItem(ItemManager.guns.get(2).copy(), 100, 950);
        WorldManager.dropItem(ItemManager.guns.get(3).copy(), 100, 1000);

        // Suelo
        for (int i = 0; i < WORLD_SIZE; i += TILE) {
            for (int j = 0; j < WORLD_SIZE; j += TILE) {
                WorldManager.build(new Struct(i, j, ItemKind.FLOOR, 3));
            }
        }

        // Muros
        for (int i = 0; i < 1200; i += TILE) {
            wall(i, TILE * 13);
        }
        wall(1000, 1000);
        wall(TILE * 10, TILE * 5);
        wall(TILE * 7, TILE * 6);
        wall(TILE * 7, TILE * 7);
        wall(TILE * 9, TILE * 7);

        WorldManager.build(new Struct(TILE * 20, TILE * 5, ItemKind.HALF_WALL, 4));
        WorldManager.build(new Struct(TILE * 20, TILE * 6, ItemKind.HALF_WALL, 4));
        WorldManager.build(new Struct(TILE * 20, TILE * 7, ItemKind.HALF_WALL, 4));

        // NPCs
        var random = ThreadLocalRandom.current();
        NpcManager.spawnNpc(NpcManager.ZOMBIE, 100, 100, 90);
        NpcManager.spawnNpc(NpcManager.HUMAN_AGGRESSIVE, 1500, 1000, random.nextInt(360));
        for (int y = 1000; y <= 1120; y += 60) {
            NpcManager.spawnNpc(NpcManager.HUMAN_REACTIVE, 2500, y, random.nextInt(360));
            NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 2700, y, random.nextInt(360));
        }
    }

    /** Oleada de zombis (arriba) y humanos agresivos (abajo). Tecla P. */
    static void spawnWave(int width) {
        var random = ThreadLocalRandom.current();
        for (int x = 0; x < width; x += 100) {
            NpcManager.spawnNpc(random.nextBoolean() ? NpcManager.ZOMBIE : NpcManager.ZOMBIE_ALT, x, 100, 0);
            NpcManager.spawnNpc(NpcManager.HUMAN_AGGRESSIVE, x, 1000, 0);
        }
    }

    private static void wall(float x, float y) {
        WorldManager.build(new Struct(x, y, ItemKind.WALL, 2));
    }
}
