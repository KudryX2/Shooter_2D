package world;

import static util.Geometry.NO_HIT;
import static util.Geometry.isEarlierHit;
import static util.Geometry.segmentEntersBox;

import items.Projectile;
import npc.Npc;
import player.Player;

/** Mueve las balas ({@link WorldManager#projectiles}) y resuelve sus impactos. */
final class Projectiles {
    /** Medio lado de la caja de impacto de jugador y NPC. */
    private static final float HITBOX = 20;
    /** El jugador recibe esta fracción del daño de la bala (si no, moriría de un par de tiros). */
    private static final int PLAYER_DAMAGE_DIVISOR = 10;

    private Projectiles() {
    }

    static void update() {
        WorldManager.projectiles.removeIf(Projectiles::update);
    }

    /**
     * Mueve la bala y resuelve sus impactos. Devuelve {@code true} si hay que eliminarla.
     *
     * <p>Una bala avanza decenas de píxeles por frame, así que no basta con mirar dónde acaba: se comprueba todo
     * el tramo recorrido y gana el primer obstáculo (muro, NPC o jugador). Así no atraviesa muros finos ni se
     * salta objetivos, y un muro protege de verdad a quien está detrás.
     */
    private static boolean update(Projectile p) {
        float x0 = p.x, y0 = p.y;
        p.move();
        float x1 = p.x, y1 = p.y;

        float first = Structures.firstWallHit(x0, y0, x1, y1);
        boolean hitsPlayer = false;
        Npc hitNpc = null;

        float t = segmentEntersBox(x0, y0, x1, y1, Player.x, Player.y, HITBOX, HITBOX);
        if (isEarlierHit(t, first)) {
            first = t;
            hitsPlayer = true;
        }
        for (Npc npc : WorldManager.npcs) {
            if (!npc.isAlive()) {
                continue;
            }
            t = segmentEntersBox(x0, y0, x1, y1, npc.x, npc.y, HITBOX, HITBOX);
            if (isEarlierHit(t, first)) {
                first = t;
                hitsPlayer = false;
                hitNpc = npc;
            }
        }

        if (hitNpc != null) {
            // Headshot: solo balas del jugador, y solo si pasan justo por donde apuntaba
            boolean throughAim = segmentEntersBox(x0, y0, x1, y1, Player.aimX, Player.aimY, HITBOX, HITBOX) != NO_HIT;
            if (p.byPlayer && throughAim) {
                hitNpc.die(true);
            } else {
                hitNpc.takeDamage(p.damage);
            }
            return true;
        }
        if (hitsPlayer) {
            Player.hp -= p.damage / PLAYER_DAMAGE_DIVISOR;
            return true;
        }
        return first != NO_HIT || p.isOutOfRange();
    }
}
