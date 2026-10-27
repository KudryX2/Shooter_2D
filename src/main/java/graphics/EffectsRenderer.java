package graphics;

import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;
import static graphics.Sprite.drawSprite;
import static graphics.TexturesManager.tex;
import static org.lwjgl.opengl.GL11.*;
import static util.Geometry.dirX;
import static util.Geometry.dirY;
import static util.Geometry.distance;

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
    /** El fogonazo empieza un poco por delante de la boca del arma. */
    private static final float FLASH_FORWARD = 14;
    private static final float FLASH_LENGTH = 22;
    private static final float FLASH_WIDTH = 6;

    private EffectsRenderer() {
    }

    /** Bala con una estela fina que se desvanece hacia atrás (nunca más larga que lo que lleva recorrido). */
    static void drawProjectiles() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(TRAIL_WIDTH);
        glBegin(GL_LINES);
        for (Projectile p : WorldManager.projectiles) {
            if (!isVisible(p.x, p.y)) {
                continue;
            }
            float dx = dirX(p.rotation);
            float dy = dirY(p.rotation);
            float length = Math.min(TRAIL_LENGTH, distance(p.x, p.y, p.startX, p.startY));
            float headX = screenX(p.x);
            float headY = screenY(p.y);
            glColor4f(1f, 0.95f, 0.75f, 0.9f);
            glVertex2f(headX, headY);
            glColor4f(1f, 0.75f, 0.35f, 0f);
            glVertex2f(headX - dx * length, headY - dy * length);
        }
        glEnd();
        glDisable(GL_LINE_SMOOTH);
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);

        float half = BULLET_SIZE / 2;
        for (Projectile p : WorldManager.projectiles) {
            if (isVisible(p.x, p.y)) {
                drawSprite(tex(TextureId.BULLET), screenX(p.x) - half, screenY(p.y) - half,
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
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
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

            glBegin(GL_TRIANGLE_FAN);
            glColor4f(1f, 1f, 0.85f, life);
            glVertex2f(cx, cy);
            glColor4f(1f, 0.6f, 0.1f, 0f);
            glVertex2f(cx + dx * length, cy + dy * length);                   // punta
            glVertex2f(cx - dy * FLASH_WIDTH, cy + dx * FLASH_WIDTH);         // lado
            glVertex2f(cx - dx * FLASH_WIDTH * 0.6f, cy - dy * FLASH_WIDTH * 0.6f); // detrás
            glVertex2f(cx + dy * FLASH_WIDTH, cy - dx * FLASH_WIDTH);         // otro lado
            glVertex2f(cx + dx * length, cy + dy * length);                   // cierra en la punta
            glEnd();
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }
}
