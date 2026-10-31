package items;

import java.util.concurrent.ThreadLocalRandom;

import util.GameClock;
import world.MuzzleFlash;
import world.Structures;
import world.WorldManager;

/**
 * Un arma concreta: sus estadísticas vienen de {@link GunType} y lleva su propio cargador y tiempo entre disparos.
 *
 * <p>Las balas del jugador salen del cargador ({@link GunType#magazine}, lleno al encontrarla) y se reponen recargando
 * ({@link #startReload}) de las cajas de su munición. Los NPC tienen munición infinita: no gastan ni recargan.
 */
public final class Gun extends Item {
    /** Abanico fijo de los perdigones de la escopeta (grados); la dispersión aleatoria se suma encima. */
    static final int[] SHOTGUN_PATTERN = {12, 6, 0, -6, -12};

    public final GunType type;
    private long lastShot = GameClock.NEVER;
    /** Balas en el cargador. */
    private int rounds;
    /** Cuándo empezó la recarga en curso, o {@link GameClock#NEVER} si no está recargando. */
    private long reloadStart = GameClock.NEVER;

    public Gun(GunType type) {
        super(type.displayName, type.icon, type.heldTexture);
        this.type = type;
        this.rounds = type.magazine;
    }

    /**
     * Intenta disparar.
     *
     * @param shotByPlayer       los NPC disparan a la mitad de cadencia y sin gastar balas
     * @param triggerJustPressed el gatillo se acaba de pulsar; las armas semiautomáticas solo disparan entonces
     */
    public void shoot(float x, float y, float rotation, boolean shotByPlayer, boolean triggerJustPressed) {
        shoot(x, y, x, y, rotation, shotByPlayer, triggerJustPressed);
    }

    /**
     * Intenta disparar desde la boca del arma (x, y), sostenida por alguien en (fromX, fromY). Si hay un muro
     * entre los dos (p.ej. disparando pegado a él) las balas chocan ahí y no aparecen al otro lado. El jugador no
     * dispara con el cargador vacío ni mientras recarga.
     */
    public void shoot(float fromX, float fromY, float x, float y, float rotation, boolean shotByPlayer,
                      boolean triggerJustPressed) {
        if (shotByPlayer && (rounds == 0 || isReloading())) {
            return;
        }
        long now = GameClock.now();
        long cooldown = shotByPlayer ? type.rate : type.rate * 2L;
        if (now - lastShot <= cooldown) {
            return;
        }
        if (!type.automatic && !triggerJustPressed) {
            return;
        }

        if (!Structures.isWallBetween(fromX, fromY, x, y)) {
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
        if (shotByPlayer) {
            rounds--;
        }
    }

    private void fire(float x, float y, float rotation, boolean byPlayer) {
        float spread = type.spread;
        float deviation = spread > 0 ? (float) ThreadLocalRandom.current().nextDouble(-spread, spread) : 0;
        WorldManager.projectiles.add(
                new Projectile(x, y, rotation + deviation, type.speed, type.range, type.damage, byPlayer,
                        type.knockback));
    }

    // ---------------------------------------------------------------------------------------------
    // Cargador y recarga
    // ---------------------------------------------------------------------------------------------

    /** Balas en el cargador. */
    public int rounds() {
        return rounds;
    }

    public boolean isReloading() {
        return reloadStart != GameClock.NEVER;
    }

    /** Cuánto lleva de la recarga en curso, de 0 a 1, o -1 si no está recargando. */
    public float reloadProgress() {
        if (!isReloading()) {
            return -1;
        }
        return Math.clamp(GameClock.since(reloadStart) / (float) type.reloadMs, 0, 1);
    }

    /**
     * Empieza a recargar, si al cargador le faltan balas, no estaba ya recargando y en {@code inventory} quedan balas
     * de su munición. Las balas no se sacan de las cajas hasta que termina ({@link #updateReload}).
     *
     * @return {@code true} si ha empezado
     */
    public boolean startReload(Inventory inventory) {
        if (isReloading() || rounds >= type.magazine || inventory.totalRounds(type.ammo) == 0) {
            return false;
        }
        reloadStart = GameClock.now();
        return true;
    }

    /**
     * Si la recarga en curso ha terminado, llena el cargador con lo que haya en las cajas de {@code inventory}. Una vez
     * por frame mientras recarga.
     *
     * @return {@code true} si acaba de terminar
     */
    public boolean updateReload(Inventory inventory) {
        if (!isReloading() || GameClock.since(reloadStart) < type.reloadMs) {
            return false;
        }
        rounds += inventory.takeRounds(type.ammo, type.magazine - rounds);
        reloadStart = GameClock.NEVER;
        return true;
    }

    /** Deja la recarga a medias (p.ej. al cambiar de arma): no se gasta nada. */
    public void cancelReload() {
        reloadStart = GameClock.NEVER;
    }
}
