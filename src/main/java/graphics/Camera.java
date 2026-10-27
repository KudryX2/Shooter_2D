package graphics;

import static util.Geometry.distance;

import player.Player;

/** La cámara sigue al jugador, que siempre está en el centro de la pantalla. */
final class Camera {

    private Camera() {
    }

    static float screenX(float worldX) {
        return worldX - Player.x + DisplayManager.WIDTH / 2f;
    }

    static float screenY(float worldY) {
        return worldY - Player.y + DisplayManager.HEIGHT / 2f;
    }

    static float centerX() {
        return DisplayManager.WIDTH / 2f;
    }

    static float centerY() {
        return DisplayManager.HEIGHT / 2f;
    }

    /** {@code true} si (worldX, worldY) está lo bastante cerca para dibujarlo. */
    static boolean isVisible(float worldX, float worldY) {
        return distance(worldX, worldY, Player.x, Player.y) < DisplayManager.WIDTH;
    }
}
