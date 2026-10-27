package input;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Estado del teclado y el ratón en el frame actual. {@link #update()} se llama una vez al principio de cada
 * frame; así todos los que pregunten en ese frame ven lo mismo, y "recién pulsado" es cierto solo en el frame en
 * que se pulsa, sin que cada clase tenga que recordar el estado anterior.
 */
public final class Input {
    private static final int MOUSE_BUTTONS = 3;

    private static final boolean[] keys = new boolean[Keyboard.KEYBOARD_SIZE];
    private static final boolean[] previousKeys = new boolean[Keyboard.KEYBOARD_SIZE];
    private static final boolean[] buttons = new boolean[MOUSE_BUTTONS];
    private static final boolean[] previousButtons = new boolean[MOUSE_BUTTONS];

    private Input() {
    }

    /** Lee el estado de este frame. Requiere la ventana creada. */
    public static void update() {
        System.arraycopy(keys, 0, previousKeys, 0, keys.length);
        System.arraycopy(buttons, 0, previousButtons, 0, buttons.length);
        for (int key = 0; key < keys.length; key++) {
            keys[key] = Keyboard.isKeyDown(key);
        }
        for (int button = 0; button < buttons.length; button++) {
            buttons[button] = Mouse.isButtonDown(button);
        }
    }

    /** La tecla está pulsada (p.ej. {@code Keyboard.KEY_W}). */
    public static boolean isKeyDown(int key) {
        return keys[key];
    }

    /** La tecla se ha pulsado en este frame. */
    public static boolean wasKeyPressed(int key) {
        return keys[key] && !previousKeys[key];
    }

    /** El botón del ratón está pulsado (0 = izquierdo, 1 = derecho). */
    public static boolean isButtonDown(int button) {
        return buttons[button];
    }

    /** El botón del ratón se ha pulsado en este frame. */
    public static boolean wasButtonPressed(int button) {
        return buttons[button] && !previousButtons[button];
    }
}
