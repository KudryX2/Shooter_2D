package npc;

import java.util.concurrent.ThreadLocalRandom;

import items.Gun;
import items.GunType;
import world.WorldManager;

public final class NpcManager {
    private static final int ZOMBIE_GUN_CHANCE_PERCENT = 20;

    private NpcManager() {
    }

    /** Crea un NPC en el mundo. Los humanos llevan pistola y algunos zombis, un arma al azar. */
    public static Npc spawn(NpcType type, float x, float y, float rotation) {
        Npc npc = new Npc(type);
        npc.setPosition(x, y, rotation);

        if (npc.kind == NpcKind.HUMAN) {
            npc.inventory.add(new Gun(GunType.PISTOL));
        } else if (ThreadLocalRandom.current().nextInt(100) < ZOMBIE_GUN_CHANCE_PERCENT) {
            npc.inventory.add(new Gun(GunType.random()));
        }

        WorldManager.npcs.add(npc);
        return npc;
    }
}
