package items;

import java.util.concurrent.ThreadLocalRandom;

import util.GameClock;
import world.MuzzleFlash;
import world.WorldManager;

/** Un arma concreta: sus estadísticas vienen de {@link GunType} y lleva su propio tiempo de recarga. */
public final class Gun extends Item {
    /** Abanico fijo de los perdigones de la escopeta (grados); la dispersión aleatoria se suma encima. */
    static final int[] SHOTGUN_PATTERN = {12, 6, 0, -6, -12};

    public final GunType type;
    private long lastShot = GameClock.NEVER;

    public Gun(GunType type) {
        super(type.displayName, type.icon, type.heldTexture);
        this.type = type;
    }

    /**
     * Intenta disparar.
     *
     * @param shotByPlayer       los NPC disparan a la mitad de cadencia
     * @param triggerJustPressed el gatillo se acaba de pulsar; las armas semiautomáticas solo disparan entonces
     */
    public void shoot(float x, float y, float rotation, boolean shotByPlayer, boolean triggerJustPressed) {
        shoot(x, y, x, y, rotation, shotByPlayer, triggerJustPressed);
    }

    /**
     * Intenta disparar desde la boca del arma (x, y), sostenida por alguien en (fromX, fromY). Si hay un muro
     * entre los dos (p.ej. disparando pegado a él) las balas chocan ahí y no aparecen al otro lado.
     */
    public void shoot(float fromX, float fromY, float x, float y, float rotation, boolean shotByPlayer,
                      boolean triggerJustPressed) {
        long now = GameClock.now();
        long cooldown = shotByPlayer ? type.rate : type.rate * 2L;
        if (now - lastShot <= cooldown) {
            return;
        }
        if (!type.automatic && !triggerJustPressed) {
            return;
        }

        if (!WorldManager.isWallBetween(fromX, fromY, x, y)) {
            if (type == GunType.SHOTGUN) {
                for (int angle : SHOTGUN_PATTERN) {
                    fire(x, y, rotation + angle, shotByPlayer);
                }
            } else {
                fire(x, y, rotation, shotByPlayer);
            }
        }
        WorldManager.muzzleFlashes.add(new MuzzleFlash(x, y, rotation, now, shotByPlayer)); // uno por disparo, también en la escopeta
        lastShot = now;
    }

    private void fire(float x, float y, float rotation, boolean byPlayer) {
        float spread = type.spread;
        float deviation = spread > 0 ? (float) ThreadLocalRandom.current().nextDouble(-spread, spread) : 0;
        WorldManager.projectiles.add(
                new Projectile(x, y, rotation + deviation, type.speed, type.range, type.damage, byPlayer));
    }
}
