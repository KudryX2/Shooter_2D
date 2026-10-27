package npc;

import static util.Geometry.distance;

import world.WorldManager;

/** Lo que decide qué hace un NPC en pie cada frame. Cada NPC tiene el suyo (puede guardar estado). */
public interface NpcBrain {

    void update(Npc npc);

    /** Zombi en pie más cercano a {@code from} dentro de {@code range}; los que se están levantando no cuentan. */
    static Npc nearestZombie(Npc from, float range) {
        Npc nearest = null;
        float best = range;
        for (Npc other : WorldManager.npcs) {
            if (other.kind != NpcKind.ZOMBIE || !other.isActive()) {
                continue;
            }
            float d = distance(from.x, from.y, other.x, other.y);
            if (d < best) {
                best = d;
                nearest = other;
            }
        }
        return nearest;
    }
}
