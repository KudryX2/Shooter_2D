package items;

import java.util.concurrent.ThreadLocalRandom;

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

    private long lastShot;

    public Gun(String name, GunKind gunKind, int cellTextureID, int equippedTextureID,
               int range, float speed, int damage, int rate, float spread) {
        super(name, ItemKind.GUN, cellTextureID, equippedTextureID);
        this.gunKind = gunKind;
        this.range = range;
        this.speed = speed;
        this.damage = damage;
        this.rate = rate;
        this.spread = spread;
    }

    /** Devuelve una copia independiente (cada arma lleva su propio tiempo de recarga). */
    public Gun copy() {
        return new Gun(name, gunKind, cellTextureID, equippedTextureID, range, speed, damage, rate, spread);
    }

    /**
     * Intenta disparar.
     *
     * @param shotByPlayer       los NPC disparan a la mitad de cadencia
     * @param triggerJustPressed el gatillo se acaba de pulsar; las armas semiautomáticas solo disparan entonces
     */
    public void shoot(float x, float y, float rotation, boolean shotByPlayer, boolean triggerJustPressed) {
        long now = System.currentTimeMillis();
        long cooldown = shotByPlayer ? rate : rate * 2L;
        if (now - lastShot <= cooldown) {
            return;
        }
        boolean automatic = gunKind == GunKind.ASSAULT_RIFLE;
        if (!automatic && !triggerJustPressed) {
            return;
        }

        switch (gunKind) {
            case SHOTGUN -> {
                for (int angle : SHOTGUN_PATTERN) {
                    fire(x, y, rotation + angle);
                }
            }
            case PISTOL, SNIPER, ASSAULT_RIFLE -> fire(x, y, rotation);
        }
        WorldManager.muzzleFlashes.add(new MuzzleFlash(x, y, rotation, now, shotByPlayer)); // uno por disparo, también en la escopeta
        lastShot = now;
    }

    private void fire(float x, float y, float rotation) {
        float deviation = spread > 0 ? (float) ThreadLocalRandom.current().nextDouble(-spread, spread) : 0;
        WorldManager.projectiles.add(new Projectile(x, y, rotation + deviation, speed, range, damage, 0));
    }
}
