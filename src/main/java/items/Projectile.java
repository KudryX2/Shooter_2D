package items;

import static util.Geometry.dirX;
import static util.Geometry.dirY;

import util.GameClock;
import util.Geometry;

/**
 * Una bala. Su posición (x, y) está en el suelo, en el mismo plano que los pies de los personajes y la base de los
 * muros (así choca con ellos), pero en la vista 3/4 vuela a la altura del pecho: se dibuja {@link #FLIGHT_HEIGHT}
 * más arriba en pantalla.
 */
public final class Projectile {
    /**
     * Altura a la que vuela: por debajo de un muro (48) y por encima de un saco terrero (24), que por eso no la para.
     * También es la altura de la boca del arma (la empuñadura queda más abajo, en las manos).
     */
    public static final float FLIGHT_HEIGHT = 32;

    public float x, y;
    public final float startX, startY, speed, rotation;
    public final int range, damage;
    /** Disparada por el jugador. Solo sus balas pueden hacer headshot. */
    public final boolean byPlayer;
    /** Cuánto aparta al NPC al que le da, en px (ver {@link GunType#knockback}). */
    public final float knockback;

    /** Bala sin empuje. */
    public Projectile(float x, float y, float rotation, float speed, int range, int damage, boolean byPlayer) {
        this(x, y, rotation, speed, range, damage, byPlayer, 0);
    }

    public Projectile(float x, float y, float rotation, float speed, int range, int damage, boolean byPlayer,
                      float knockback) {
        this.knockback = knockback;
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.rotation = rotation;
        this.speed = speed;
        this.range = range;
        this.damage = damage;
        this.byPlayer = byPlayer;
    }

    public void move() {
        float step = speed * GameClock.delta();
        x += dirX(rotation) * step;
        y += dirY(rotation) * step;
    }

    public boolean isOutOfRange() {
        return Geometry.distance(x, y, startX, startY) > range;
    }
}
