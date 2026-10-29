package graphics;

import static util.Geometry.distance;

import player.Player;

/**
 * La cámara sigue al jugador, que siempre está en el centro de la pantalla.
 *
 * <p>El mundo se dibuja en "coordenadas de pantalla sin zoom" ({@link #screenX}, {@link #screenY}); la cámara
 * del mundo ({@link Draw}) aplica luego el zoom ({@link DisplayManager#worldZoom()}) alrededor del centro.
 */
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

    /**
     * Cuánto se ha metido el jugador dentro de su píxel del pixel art (de 0 a {@link DisplayManager#ART_PIXEL}), en
     * unidades del mundo. Con {@link DisplayManager#PIXEL_PERFECT} el mundo se dibuja alineado a esos píxeles y este
     * resto se compensa al pasarlo a pantalla.
     */
    static float offsetInPixelX() {
        return offsetInPixel(Player.x);
    }

    static float offsetInPixelY() {
        return offsetInPixel(Player.y);
    }

    static float offsetInPixel(float coordinate) {
        float art = DisplayManager.ART_PIXEL;
        return coordinate - (float) Math.floor(coordinate / art) * art;
    }

    /** {@code true} si (worldX, worldY) está lo bastante cerca para dibujarlo. */
    static boolean isVisible(float worldX, float worldY) {
        return distance(worldX, worldY, Player.x, Player.y) < DisplayManager.WIDTH * DisplayManager.worldZoom();
    }
}
