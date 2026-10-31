package npc.brain;

import static util.Geometry.distance;

import java.util.concurrent.ThreadLocalRandom;

import npc.Npc;
import player.Player;
import util.GameClock;

/** Persigue y muerde al jugador si está cerca; si no, deambula alrededor de donde empezó. */
public final class ZombieBrain implements NpcBrain {
    private static final float CHASE_RANGE = 600;
    /** Se para a esta distancia del jugador para no meterse encima. */
    public static final float ATTACK_DISTANCE = 55;
    /** Algo más que ATTACK_DISTANCE para que el mordisco llegue aunque el jugador se aleje un poco. */
    public static final float BITE_RANGE = 65;
    private static final int BITE_DAMAGE = 10;
    private static final long BITE_COOLDOWN_MS = 800;
    /**
     * Tiempo seguido en rango antes de morder: da margen para apartarse o golpearle. Se reinicia al salir del rango
     * y al quedar aturdido (un golpe cuerpo a cuerpo cancela el mordisco que estaba preparando).
     */
    public static final long BITE_WINDUP_MS = 200;
    private static final float WANDER_RADIUS = 200;
    /** Al deambular va a esta fracción de su velocidad. */
    private static final float WANDER_SPEED_FACTOR = 0.2f;

    private boolean hasHome;
    private float homeX, homeY;
    private boolean wandering;
    private long lastBite = GameClock.NEVER;
    /** Desde cuándo está seguido en rango de morder; {@link GameClock#NEVER} si no lo está. */
    private long inRangeSince = GameClock.NEVER;

    @Override
    public void update(Npc npc) {
        if (!hasHome) { // donde empieza a moverse (al aparecer o al levantarse) es su zona
            hasHome = true;
            homeX = npc.x;
            homeY = npc.y;
        }

        npc.setAttackCharge(0); // solo lo prepara mientras está en rango (lo pone bite)
        float playerDistance = distance(npc.x, npc.y, Player.x, Player.y);
        if (playerDistance < CHASE_RANGE) {
            wandering = false;
            npc.lookAt(Player.x, Player.y);
            if (playerDistance < BITE_RANGE) {
                bite(npc);
            } else {
                inRangeSince = GameClock.NEVER;
            }
            if (playerDistance > ATTACK_DISTANCE) {
                npc.walk(1);
            }
        } else {
            if (!wandering) {
                wandering = true;
                npc.rotation = ThreadLocalRandom.current().nextInt(360);
            }
            if (distance(npc.x, npc.y, homeX, homeY) > WANDER_RADIUS) {
                npc.lookAt(homeX, homeY);
            }
            npc.walk(WANDER_SPEED_FACTOR);
        }
    }

    /**
     * Ataque cuerpo a cuerpo al jugador, tras {@link #BITE_WINDUP_MS} en rango y como mucho una vez cada
     * {@link #BITE_COOLDOWN_MS}. Durante los {@link #BITE_WINDUP_MS} anteriores a cada mordisco (también a los
     * siguientes al primero) va marcando en el NPC lo a punto que está, para que se vea venir.
     */
    private void bite(Npc npc) {
        long now = GameClock.now();
        if (inRangeSince == GameClock.NEVER) {
            inRangeSince = now;
        }
        // Mientras está aturdido no se le llama, así que se cuenta desde que se le pasó
        long preparingSince = Math.max(inRangeSince, npc.stunnedUntil());
        long nextBite = Math.max(preparingSince + BITE_WINDUP_MS, lastBite + BITE_COOLDOWN_MS);
        if (now >= nextBite) {
            Player.hp -= BITE_DAMAGE;
            lastBite = now;
            npc.markAttack();
        } else {
            npc.setAttackCharge(1 - (nextBite - now) / (float) BITE_WINDUP_MS);
        }
    }
}
