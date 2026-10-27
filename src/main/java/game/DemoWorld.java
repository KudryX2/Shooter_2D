package game;

import java.util.concurrent.ThreadLocalRandom;

import assets.TextureId;
import items.Consumable;
import items.Gun;
import items.GunType;
import items.StructItem;
import npc.NpcManager;
import npc.NpcType;
import player.Player;
import structs.Struct;
import structs.StructKind;
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
        Player.inventory.add(new Gun(GunType.PISTOL));

        for (int i = 0; i < WORLD_SIZE; i += CHUNK_SIZE) {
            for (int j = 0; j < WORLD_SIZE; j += CHUNK_SIZE) {
                WorldManager.chunks.add(new Chunk(i, j));
            }
        }
        System.out.println("Se han creado " + WorldManager.chunks.size() + " chunks");

        // Objetos en el suelo
        WorldManager.dropItem(new StructItem(StructKind.WALL), -100, 500);
        WorldManager.dropItem(new StructItem(StructKind.HALF_WALL), -100, 550);
        WorldManager.dropItem(new StructItem(StructKind.FLOOR), -100, 600);
        WorldManager.dropItem(new Gun(GunType.SHOTGUN), 100, 900);
        WorldManager.dropItem(new Gun(GunType.SNIPER), 100, 950);
        WorldManager.dropItem(new Gun(GunType.ASSAULT_RIFLE), 100, 1000);
        WorldManager.dropItem(Consumable.medkit(), 100, 1050);
        WorldManager.dropItem(Consumable.medkit(), 100, 1100);

        // Suelo
        for (int i = 0; i < WORLD_SIZE; i += TILE) {
            for (int j = 0; j < WORLD_SIZE; j += TILE) {
                WorldManager.build(new Struct(i, j, StructKind.FLOOR, TextureId.GRASS));
            }
        }

        // Muros
        for (int i = 0; i < 1200 / TILE; i++) {
            WorldManager.build(Struct.northWall(i, 13, StructKind.WALL));
        }
        WorldManager.build(Struct.northWall(15, 15, StructKind.WALL));
        WorldManager.build(Struct.northWall(10, 5, StructKind.WALL));
        WorldManager.build(Struct.westWall(7, 6, StructKind.WALL));
        WorldManager.build(Struct.westWall(7, 7, StructKind.WALL));
        WorldManager.build(Struct.northWall(9, 7, StructKind.WALL));

        for (int j = 5; j <= 7; j++) {
            WorldManager.build(Struct.westWall(20, j, StructKind.HALF_WALL));
        }

        // NPCs
        var random = ThreadLocalRandom.current();
        NpcManager.spawn(NpcType.ZOMBIE, 100, 100, 90);
        NpcManager.spawn(NpcType.SOLDIER, 1500, 1000, random.nextInt(360));
        for (int y = 1000; y <= 1120; y += 60) {
            NpcManager.spawn(NpcType.GUARD, 2500, y, random.nextInt(360));
            NpcManager.spawn(NpcType.CIVILIAN, 2700, y, random.nextInt(360));
        }
    }

    /** Oleada de zombis (arriba) y humanos agresivos (abajo). Tecla P. */
    static void spawnWave(int width) {
        var random = ThreadLocalRandom.current();
        for (int x = 0; x < width; x += 100) {
            NpcManager.spawn(random.nextBoolean() ? NpcType.ZOMBIE : NpcType.ZOMBIE_ALT, x, 100, 0);
            NpcManager.spawn(NpcType.SOLDIER, x, 1000, 0);
        }
    }
}
