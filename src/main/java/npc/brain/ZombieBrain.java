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
    private static final float WANDER_RADIUS = 200;
    /** Al deambular va a esta fracción de su velocidad. */
    private static final float WANDER_SPEED_FACTOR = 0.2f;

    private boolean hasHome;
    private float homeX, homeY;
    private boolean wandering;
    private long lastBite = GameClock.NEVER;

    @Override
    public void update(Npc npc) {
        if (!hasHome) { // donde empieza a moverse (al aparecer o al levantarse) es su zona
            hasHome = true;
            homeX = npc.x;
            homeY = npc.y;
        }

        float playerDistance = distance(npc.x, npc.y, Player.x, Player.y);
        if (playerDistance < CHASE_RANGE) {
            wandering = false;
            npc.lookAt(Player.x, Player.y);
            if (playerDistance < BITE_RANGE) {
                bite();
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

    /** Ataque cuerpo a cuerpo al jugador, como mucho una vez cada {@link #BITE_COOLDOWN_MS}. */
    private void bite() {
        long now = GameClock.now();
        if (now - lastBite >= BITE_COOLDOWN_MS) {
            Player.hp -= BITE_DAMAGE;
            lastBite = now;
        }
    }
}
