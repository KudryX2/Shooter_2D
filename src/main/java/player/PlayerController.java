package player;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import graphics.DisplayManager;
import input.Input;

/** Traduce el teclado y el ratón en acciones del {@link Player}. Una llamada a {@link #update} por frame. */
public final class PlayerController {
    private static final int[] SLOT_KEYS = {Keyboard.KEY_1, Keyboard.KEY_2, Keyboard.KEY_3, Keyboard.KEY_4};

    /** El clic izquierdo actual empezó sobre un botón del HUD: no dispara ni construye hasta soltarlo. */
    private static boolean clickStartedOnUi;

    private PlayerController() {
    }

    /** Centra el cursor en la pantalla (sobre el jugador). Requiere la ventana creada. */
    public static void init() {
        Mouse.setCursorPosition(DisplayManager.WIDTH / 2, DisplayManager.HEIGHT / 2);
    }

    /** @param mouseOnUi el cursor está sobre un botón del HUD */
    public static void update(boolean mouseOnUi) {
        int dx = (Input.isKeyDown(Keyboard.KEY_D) ? 1 : 0) - (Input.isKeyDown(Keyboard.KEY_A) ? 1 : 0);
        int dy = (Input.isKeyDown(Keyboard.KEY_S) ? 1 : 0) - (Input.isKeyDown(Keyboard.KEY_W) ? 1 : 0);
        Player.move(dx, dy);
        Player.aimAt(cursorWorldX(), cursorWorldY());

        updateSelectedSlot();
        Player.updateBuildPreview();

        if (Input.wasButtonPressed(0)) clickStartedOnUi = mouseOnUi;
        if (Input.isButtonDown(0) && !clickStartedOnUi) Player.primaryAction(Input.wasButtonPressed(0));
        if (Input.wasButtonPressed(1)) Player.secondaryAction();
        if (Input.wasKeyPressed(Keyboard.KEY_Q)) Player.dropSelectedItem();
        if (Input.wasKeyPressed(Keyboard.KEY_E)) Player.pickUpItem();

        Player.update();
    }

    /** Teclas 1-4 o rueda del ratón. */
    private static void updateSelectedSlot() {
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            if (Input.isKeyDown(SLOT_KEYS[slot])) {
                Player.inventory.select(slot);
            }
        }
        int wheel = Mouse.getDWheel();
        if (wheel < 0) {
            Player.inventory.cycle(1);
        } else if (wheel > 0) {
            Player.inventory.cycle(-1);
        }
    }

    /** Punto del mundo bajo el cursor (el jugador siempre está en el centro de la pantalla). */
    private static float cursorWorldX() {
        return Player.x + Mouse.getX() - DisplayManager.WIDTH / 2f;
    }

    private static float cursorWorldY() {
        return Player.y + DisplayManager.HEIGHT / 2f - Mouse.getY();
    }
}
