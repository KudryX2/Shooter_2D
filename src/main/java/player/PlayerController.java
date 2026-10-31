package player;

import com.badlogic.gdx.Input.Keys;

import graphics.DisplayManager;
import input.Input;

/** Traduce el teclado y el ratón en acciones del {@link Player}. Una llamada a {@link #update} por frame. */
public final class PlayerController {
    /** Teclas 1-9 y 0 (la décima casilla), en el orden de las casillas. */
    private static final int[] SLOT_KEYS = {
            Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.NUM_5,
            Keys.NUM_6, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9, Keys.NUM_0};

    /** El clic izquierdo actual empezó sobre un botón del HUD: no dispara ni construye hasta soltarlo. */
    private static boolean clickStartedOnUi;

    private PlayerController() {
    }

    /** Centra el cursor en la pantalla (sobre el jugador). */
    public static void init() {
        Input.centerCursor();
    }

    /**
     * @param mouseOnUi el ratón es para el HUD (está sobre un botón o el inventario está abierto): no dispara, no
     *                  construye ni usa objetos
     */
    public static void update(boolean mouseOnUi) {
        int dx = (Input.isKeyDown(Keys.D) ? 1 : 0) - (Input.isKeyDown(Keys.A) ? 1 : 0);
        int dy = (Input.isKeyDown(Keys.S) ? 1 : 0) - (Input.isKeyDown(Keys.W) ? 1 : 0);
        Player.move(dx, dy);
        Player.aimAt(cursorWorldX(), cursorWorldY());

        updateSelectedSlot();
        Player.updateBuildPreview();

        if (Input.wasButtonPressed(0)) clickStartedOnUi = mouseOnUi;
        if (Input.isButtonDown(0) && !clickStartedOnUi) Player.primaryAction(Input.wasButtonPressed(0));
        if (Input.wasButtonPressed(1) && !mouseOnUi) Player.secondaryAction();
        if (Input.wasKeyPressed(Keys.Q)) Player.dropSelectedItem();
        if (Input.wasKeyPressed(Keys.E)) Player.pickUpItem();

        Player.update();
    }

    /** Teclas 1-9 y 0, o rueda del ratón (hacia el jugador = casilla siguiente). */
    private static void updateSelectedSlot() {
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            if (Input.isKeyDown(SLOT_KEYS[slot])) {
                Player.inventory.select(slot);
            }
        }
        float scroll = Input.scroll();
        if (scroll > 0) {
            Player.inventory.cycle(1);
        } else if (scroll < 0) {
            Player.inventory.cycle(-1);
        }
    }

    /** Punto del mundo bajo el cursor (el jugador siempre está en el centro de la pantalla; el mundo, con zoom). */
    private static float cursorWorldX() {
        return Player.x + (Input.mouseX() - DisplayManager.WIDTH / 2f) * DisplayManager.worldZoom();
    }

    private static float cursorWorldY() {
        return Player.y + (Input.mouseY() - DisplayManager.HEIGHT / 2f) * DisplayManager.worldZoom();
    }
}
