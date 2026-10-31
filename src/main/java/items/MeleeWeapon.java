package items;

import static util.Geometry.angleTo;
import static util.Geometry.distance;

import npc.Npc;
import util.GameClock;
import world.Structures;
import world.WorldManager;

/**
 * Un arma cuerpo a cuerpo concreta: sus estadísticas vienen de {@link MeleeType} y lleva su propio tiempo entre
 * golpes. El golpe es instantáneo: da a todos los que estén dentro de su arco y su alcance, salvo que haya un muro en
 * medio. Luego el arma tarda {@link #SWING_MS} en barrer el arco, pero eso es solo la animación.
 */
public final class MeleeWeapon extends Item {
    /** Lo que dura la animación del golpe. */
    public static final long SWING_MS = 180;

    public final MeleeType type;
    private long lastSwing = GameClock.NEVER;

    public MeleeWeapon(MeleeType type) {
        super(type.displayName, type.icon, type.heldTexture);
        this.type = type;
    }

    /**
     * Intenta dar un golpe desde (x, y) hacia {@code rotation}. Se puede mantener pulsado: golpea cada
     * {@link MeleeType#rate}.
     *
     * @return {@code true} si ha golpeado (aunque no le haya dado a nadie)
     */
    public boolean swing(float x, float y, float rotation) {
        long now = GameClock.now();
        if (now - lastSwing < type.rate) {
            return false;
        }
        lastSwing = now;
        for (Npc npc : WorldManager.npcs) {
            if (npc.isAlive() && isInArc(x, y, rotation, npc.x, npc.y)
                    && !Structures.isWallBetween(x, y, npc.x, npc.y)) {
                npc.takeDamage(type.damage);
                npc.stun(type.stun); // así un zombi golpeado no muerde
                npc.push(angleTo(x, y, npc.x, npc.y), type.knockback);
            }
        }
        return true;
    }

    /** (targetX, targetY) está al alcance y dentro del arco de un golpe desde (x, y) hacia {@code rotation}. */
    boolean isInArc(float x, float y, float rotation, float targetX, float targetY) {
        float distance = distance(x, y, targetX, targetY);
        if (distance > type.reach) {
            return false;
        }
        // Pegado a él (o encima) siempre le da, mire hacia donde mire
        return distance < 1 || Math.abs(angleBetween(rotation, angleTo(x, y, targetX, targetY))) <= type.arc / 2;
    }

    /** Diferencia entre dos ángulos, de -180 a 180 grados. */
    static float angleBetween(float a, float b) {
        float difference = (b - a) % 360;
        if (difference > 180) difference -= 360;
        if (difference < -180) difference += 360;
        return difference;
    }

    /**
     * Cuánto lleva la animación del último golpe, de 0 (acaba de golpear) a 1 (ha terminado o no ha golpeado
     * nunca).
     */
    public float swingProgress() {
        long elapsed = GameClock.now() - lastSwing;
        return lastSwing == GameClock.NEVER ? 1 : Math.clamp(elapsed / (float) SWING_MS, 0, 1);
    }
}
