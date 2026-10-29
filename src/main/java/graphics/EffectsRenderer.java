package graphics;

import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;
import static util.Geometry.dirX;
import static util.Geometry.dirY;
import static util.Geometry.distance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import items.Projectile;
import player.Player;
import util.GameClock;
import world.MuzzleFlash;
import world.WorldManager;

/** Efectos de disparo: balas con su estela y fogonazos. */
final class EffectsRenderer {
    private static final float BULLET_SIZE = 4;
    private static final float TRAIL_LENGTH = 22;
    private static final float TRAIL_WIDTH = 1.5f;
    private static final Color TRAIL_HEAD = new Color(1f, 0.95f, 0.75f, 0.9f);
    private static final Color TRAIL_TAIL = new Color(1f, 0.75f, 0.35f, 0f);
    /** El fogonazo empieza un poco por delante de la boca del arma. */
    private static final float FLASH_FORWARD = 14;
    private static final float FLASH_LENGTH = 22;
    private static final float FLASH_WIDTH = 6;
    private static final Color FLASH_EDGE = new Color(1f, 0.6f, 0.1f, 0f);
    /** Centro del fogonazo; su opacidad cambia con el tiempo. */
    private static final Color flashCenter = new Color(1f, 1f, 0.85f, 1f);

    private EffectsRenderer() {
    }

    /**
     * Bala con una estela fina que se desvanece hacia atrás (nunca más larga que lo que lleva recorrido). Se dibuja
     * a la altura a la que vuela ({@link Projectile#FLIGHT_HEIGHT} sobre su posición en el suelo).
     */
    static void drawProjectiles() {
        ShapeRenderer shapes = Draw.shapes();
        for (Projectile p : WorldManager.projectiles) {
            if (!isVisible(p.x, p.y)) {
                continue;
            }
            float length = Math.min(TRAIL_LENGTH, distance(p.x, p.y, p.startX, p.startY));
            float headX = screenX(p.x);
            float headY = screenY(p.y) - Projectile.FLIGHT_HEIGHT;
            shapes.rectLine(headX, headY, headX - dirX(p.rotation) * length, headY - dirY(p.rotation) * length,
                    TRAIL_WIDTH, TRAIL_HEAD, TRAIL_TAIL);
        }

        float half = BULLET_SIZE / 2;
        for (Projectile p : WorldManager.projectiles) {
            if (isVisible(p.x, p.y)) {
                Draw.sprite(TextureId.BULLET, screenX(p.x) - half, screenY(p.y) - Projectile.FLIGHT_HEIGHT - half,
                        BULLET_SIZE, BULLET_SIZE, p.rotation);
            }
        }
    }

    /**
     * Destello en forma de rombo alargado hacia delante, con mezcla aditiva y que se apaga en
     * {@link MuzzleFlash#DURATION_MS}. El del jugador va pegado a su mano aunque se mueva.
     */
    static void drawMuzzleFlashes() {
        long now = GameClock.now();
        Draw.setAdditive(true);
        for (MuzzleFlash flash : WorldManager.muzzleFlashes) {
            float life = 1 - (now - flash.time()) / (float) MuzzleFlash.DURATION_MS;
            if (life <= 0) {
                continue;
            }
            float x, y, rotation;
            if (flash.byPlayer()) {
                rotation = Player.rotation;
                x = CharacterRenderer.handX();
                y = CharacterRenderer.handY();
            } else {
                if (!isVisible(flash.x(), flash.y())) {
                    continue;
                }
                rotation = flash.rotation();
                x = screenX(flash.x());
                y = screenY(flash.y());
            }
            float dx = dirX(rotation);
            float dy = dirY(rotation);
            float cx = x + dx * FLASH_FORWARD;
            float cy = y + dy * FLASH_FORWARD;
            float length = FLASH_LENGTH * (0.6f + 0.4f * life);

            // Rombo: cuatro triángulos desde el centro (brillante) hasta los bordes (transparentes)
            float tipX = cx + dx * length, tipY = cy + dy * length;
            float side1X = cx - dy * FLASH_WIDTH, side1Y = cy + dx * FLASH_WIDTH;
            float backX = cx - dx * FLASH_WIDTH * 0.6f, backY = cy - dy * FLASH_WIDTH * 0.6f;
            float side2X = cx + dy * FLASH_WIDTH, side2Y = cy - dx * FLASH_WIDTH;
            flashCenter.a = life;
            ShapeRenderer shapes = Draw.shapes();
            shapes.triangle(cx, cy, tipX, tipY, side1X, side1Y, flashCenter, FLASH_EDGE, FLASH_EDGE);
            shapes.triangle(cx, cy, side1X, side1Y, backX, backY, flashCenter, FLASH_EDGE, FLASH_EDGE);
            shapes.triangle(cx, cy, backX, backY, side2X, side2Y, flashCenter, FLASH_EDGE, FLASH_EDGE);
            shapes.triangle(cx, cy, side2X, side2Y, tipX, tipY, flashCenter, FLASH_EDGE, FLASH_EDGE);
        }
        Draw.setAdditive(false);
    }
}
