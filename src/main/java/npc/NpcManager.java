package npc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import items.ItemManager;
import world.WorldManager;

public final class NpcManager {
    // Tipos de NPC (índices de las plantillas)
    public static final int ZOMBIE = 0;
    public static final int ZOMBIE_ALT = 1;
    public static final int HUMAN_AGGRESSIVE = 2;
    public static final int HUMAN_REACTIVE = 3;
    public static final int HUMAN_PASSIVE = 4;

    private static final int ZOMBIE_GUN_CHANCE_PERCENT = 20;
    private static final List<Npc> templates = new ArrayList<>();

    private NpcManager() {
    }

    public static void init() {
        templates.clear();
        templates.add(new Npc(0.25f, Npc.TEXTURE_ZOMBIE, NpcKind.ZOMBIE, NpcBehaviour.AGGRESSIVE));     // ZOMBIE
        templates.add(new Npc(0.25f, Npc.TEXTURE_ZOMBIE_ALT, NpcKind.ZOMBIE, NpcBehaviour.AGGRESSIVE)); // ZOMBIE_ALT
        templates.add(new Npc(0.25f, Npc.TEXTURE_HUMAN, NpcKind.HUMAN, NpcBehaviour.AGGRESSIVE));       // HUMAN_AGGRESSIVE
        templates.add(new Npc(0.25f, Npc.TEXTURE_HUMAN, NpcKind.HUMAN, NpcBehaviour.REACTIVE));         // HUMAN_REACTIVE
        templates.add(new Npc(0.25f, Npc.TEXTURE_HUMAN, NpcKind.HUMAN, NpcBehaviour.PASSIVE));          // HUMAN_PASSIVE
    }

    public static Npc spawnNpc(int type, float x, float y, float rotation) {
        Npc npc = new Npc(templates.get(type));
        npc.setPosition(x, y, rotation);

        if (npc.kind == NpcKind.HUMAN) {
            npc.inventory.add(ItemManager.guns.getFirst().copy());
        } else if (ThreadLocalRandom.current().nextInt(100) < ZOMBIE_GUN_CHANCE_PERCENT) {
            npc.inventory.add(ItemManager.randomGun().copy());
        }

        WorldManager.npcs.add(npc);
        return npc;
    }
}
