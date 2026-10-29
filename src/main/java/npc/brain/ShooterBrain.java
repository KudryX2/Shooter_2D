package npc.brain;

import static util.Geometry.distance;

import npc.Npc;
import player.Player;

/** Humano armado que se queda en su sitio y dispara a los zombis y, si es hostil, también al jugador. */
public final class ShooterBrain implements NpcBrain {
    private final float zombieRange;
    /** 0 = nunca dispara al jugador. */
    private final float playerRange;

    ShooterBrain(float zombieRange, float playerRange) {
        this.zombieRange = zombieRange;
        this.playerRange = playerRange;
    }

    /** Dispara a zombis y, si no hay, al jugador. */
    public static ShooterBrain soldier() {
        return new ShooterBrain(800, 400);
    }

    /** Aliado: solo dispara a zombis. */
    public static ShooterBrain guard() {
        return new ShooterBrain(800, 0);
    }

    @Override
    public void update(Npc npc) {
        Npc zombie = NpcBrain.nearestZombie(npc, zombieRange);
        if (zombie != null) {
            npc.lookAt(zombie.x, zombie.y);
            npc.shoot();
        } else if (distance(npc.x, npc.y, Player.x, Player.y) < playerRange) {
            npc.lookAt(Player.x, Player.y);
            npc.shoot();
        }
    }
}
