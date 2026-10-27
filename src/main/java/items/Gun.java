package items;

import java.util.concurrent.ThreadLocalRandom;

import assets.TextureId;
import util.GameClock;
import world.MuzzleFlash;
import world.WorldManager;

public final class Gun extends Item {
    /** Abanico fijo de los perdigones de la escopeta (grados); la dispersión aleatoria se suma encima. */
    private static final int[] SHOTGUN_PATTERN = {12, 6, 0, -6, -12};

    public final GunKind gunKind;
    public final int range;
    public final float speed;
    public final int damage;
    /** Tiempo mínimo entre disparos, en milisegundos. */
    public final int rate;
    /** Desviación aleatoria máxima de cada bala, en grados a cada lado (0 = disparo perfecto). */
    public final float spread;

    private long lastShot = GameClock.NEVER;

    public Gun(String name, GunKind gunKind, TextureId icon, TextureId heldTexture,
               int range, float speed, int damage, int rate, float spread) {
        super(name, ItemKind.GUN, icon, heldTexture);
        this.gunKind = gunKind;
        this.range = range;
        this.speed = speed;
        this.damage = damage;
        this.rate = rate;
        this.spread = spread;
    }

    /** Devuelve una copia independiente (cada arma lleva su propio tiempo de recarga). */
    public Gun copy() {
        return new Gun(name, gunKind, icon, heldTexture, range, speed, damage, rate, spread);
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
        long cooldown = shotByPlayer ? rate : rate * 2L;
        if (now - lastShot <= cooldown) {
            return;
        }
        boolean automatic = gunKind == GunKind.ASSAULT_RIFLE;
        if (!automatic && !triggerJustPressed) {
            return;
        }

        boolean blocked = WorldManager.isWallBetween(fromX, fromY, x, y);
        if (!blocked) {
            switch (gunKind) {
                case SHOTGUN -> {
                    for (int angle : SHOTGUN_PATTERN) {
                        fire(x, y, rotation + angle, shotByPlayer);
                    }
                }
                case PISTOL, SNIPER, ASSAULT_RIFLE -> fire(x, y, rotation, shotByPlayer);
            }
        }
        WorldManager.muzzleFlashes.add(new MuzzleFlash(x, y, rotation, now, shotByPlayer)); // uno por disparo, también en la escopeta
        lastShot = now;
    }

    private void fire(float x, float y, float rotation, boolean byPlayer) {
        float deviation = spread > 0 ? (float) ThreadLocalRandom.current().nextDouble(-spread, spread) : 0;
        WorldManager.projectiles.add(new Projectile(x, y, rotation + deviation, speed, range, damage, byPlayer));
    }
}
