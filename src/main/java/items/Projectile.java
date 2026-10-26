package items;

import graphics.DisplayManager;
import util.Geometry;

public final class Projectile {
    public float x, y;
    public final float startX, startY, speed, rotation;
    public final int range, damage, textureID;

    public Projectile(float x, float y, float rotation, float speed, int range, int damage, int textureID) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.rotation = rotation;
        this.speed = speed;
        this.range = range;
        this.damage = damage;
        this.textureID = textureID;
    }

    public void move() {
        double angle = Math.toRadians(-rotation + 90);
        float step = speed * DisplayManager.delta;
        x += (float) (Math.sin(angle) * step);
        y += (float) (Math.cos(angle) * step);
    }

    public boolean isOutOfRange() {
        return Geometry.distance(x, y, startX, startY) > range;
    }
}
