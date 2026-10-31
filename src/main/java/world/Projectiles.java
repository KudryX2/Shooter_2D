package world;

import static util.Geometry.NO_HIT;
import static util.Geometry.isEarlierHit;
import static util.Geometry.segmentEntersBox;

import items.Projectile;
import npc.Npc;
import player.Player;

/**
 * Mueve las balas ({@link WorldManager#projectiles}) y resuelve sus impactos.
 *
 * <p>En la vista 3/4 un personaje está en el suelo por sus pies pero se dibuja hacia arriba, y las balas vuelan a
 * la altura del pecho ({@link Projectile#FLIGHT_HEIGHT}). Por eso la caja de impacto no está centrada en sus pies:
 * cubre las balas que, tal y como se dibujan, le pasan por el cuerpo (de la cabeza a las rodillas). Las medidas
 * cuadran con el sprite de 12x20 px de {@code graphics.CharacterRenderer}.
 */
final class Projectiles {
    /** Caja de impacto de jugador y NPC, relativa a sus pies: medio ancho, y cuánto sube y baja. */
    static final float HIT_HALF_WIDTH = 20;
    static final float HIT_ABOVE = 36;
    static final float HIT_BELOW = 20;
    /** Su cabeza tal y como se dibuja, relativa a sus pies (para los headshots, que dependen del cursor). */
    static final float HEAD_HALF_WIDTH = 16;
    static final float HEAD_TOP = 68;
    static final float HEAD_BOTTOM = 40;
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

        if (!p.byPlayer) { // las del jugador salen de su mano: no se las come él mismo
            float t = hitBody(x0, y0, x1, y1, Player.x, Player.y);
            if (isEarlierHit(t, first)) {
                first = t;
                hitsPlayer = true;
            }
        }
        for (Npc npc : WorldManager.npcs) {
            if (!npc.isAlive()) {
                continue;
            }
            float t = hitBody(x0, y0, x1, y1, npc.x, npc.y);
            if (isEarlierHit(t, first)) {
                first = t;
                hitsPlayer = false;
                hitNpc = npc;
            }
        }

        if (hitNpc != null) {
            // Headshot: solo balas del jugador, y solo si apuntaba a la cabeza que se ve
            if (p.byPlayer && aimsAtHead(hitNpc)) {
                hitNpc.die(true);
            } else {
                hitNpc.takeDamage(p.damage);
                hitNpc.push(p.rotation, p.knockback); // en la dirección de la bala; al jugador no se le empuja
            }
            return true;
        }
        if (hitsPlayer) {
            Player.hp -= p.damage / PLAYER_DAMAGE_DIVISOR;
            return true;
        }
        return first != NO_HIT || p.isOutOfRange();
    }

    /** Dónde entra el tramo en la caja de impacto de alguien con los pies en (x, y), o {@code NO_HIT}. */
    private static float hitBody(float x0, float y0, float x1, float y1, float x, float y) {
        float halfHeight = (HIT_ABOVE + HIT_BELOW) / 2;
        float centerY = y + (HIT_BELOW - HIT_ABOVE) / 2;
        return segmentEntersBox(x0, y0, x1, y1, x, centerY, HIT_HALF_WIDTH, halfHeight);
    }

    /** El cursor del jugador está sobre la cabeza del NPC tal y como se dibuja. */
    static boolean aimsAtHead(Npc npc) {
        return Math.abs(Player.aimX - npc.x) <= HEAD_HALF_WIDTH
                && Player.aimY >= npc.y - HEAD_TOP
                && Player.aimY <= npc.y - HEAD_BOTTOM;
    }
}
