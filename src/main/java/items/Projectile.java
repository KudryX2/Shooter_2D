package items;

import util.GameClock;
import util.Geometry;

public final class Projectile {
    public float x, y;
    public final float startX, startY, speed, rotation;
    public final int range, damage;
    /** Disparada por el jugador. Solo sus balas pueden hacer headshot. */
    public final boolean byPlayer;

    public Projectile(float x, float y, float rotation, float speed, int range, int damage, boolean byPlayer) {
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
        double angle = Math.toRadians(-rotation + 90);
        float step = speed * GameClock.delta();
        x += (float) (Math.sin(angle) * step);
        y += (float) (Math.cos(angle) * step);
    }

    public boolean isOutOfRange() {
        return Geometry.distance(x, y, startX, startY) > range;
    }
}
