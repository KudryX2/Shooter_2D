package input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

import graphics.DisplayManager;

/**
 * Estado del teclado y el ratón en el frame actual. {@link #update()} se llama una vez al principio de cada
 * frame; así todos los que pregunten en ese frame ven lo mismo, y "recién pulsado" es cierto solo en el frame en
 * que se pulsa, sin que cada clase tenga que recordar el estado anterior.
 *
 * <p>Las teclas son las de libGDX ({@link Keys}); los botones del ratón, 0 = izquierdo y 1 = derecho. Mientras
 * la ventana está activa el cursor del sistema está capturado: el juego lleva su propio cursor, que no sale de
 * la ventana. Sus coordenadas son de pantalla, con el origen arriba a la izquierda.
 */
public final class Input {
    private static final int MOUSE_BUTTONS = 3;

    private static final boolean[] keys = new boolean[Keys.MAX_KEYCODE + 1];
    private static final boolean[] previousKeys = new boolean[Keys.MAX_KEYCODE + 1];
    private static final boolean[] buttons = new boolean[MOUSE_BUTTONS];
    private static final boolean[] previousButtons = new boolean[MOUSE_BUTTONS];

    private static float mouseX, mouseY;
    /** Giro de la rueda acumulado desde el último frame, y el de este frame. */
    private static float pendingScroll, scroll;

    /** La ventana está activa (delante): solo entonces se captura el ratón. */
    private static boolean focused;
    /** El juego ha pedido soltar el ratón (p.ej. en pausa) aunque la ventana esté activa. */
    private static boolean released;
    /** Estado aplicado ahora mismo al cursor del sistema. */
    private static boolean captured;
    /** Tras volver a la ventana, el clic con el que se ha vuelto no cuenta hasta soltarlo. */
    private static boolean ignoreButtonsUntilReleased;

    private Input() {
    }

    /** Empieza a escuchar la rueda y captura el cursor si la ventana está activa. Requiere la ventana creada. */
    public static void init() {
        applyCursorCapture();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                pendingScroll += amountY;
                return true;
            }
        });
        centerCursor();
    }

    public static void centerCursor() {
        mouseX = DisplayManager.WIDTH / 2f;
        mouseY = DisplayManager.HEIGHT / 2f;
    }

    /**
     * Avisa de que la ventana se ha activado o ha quedado detrás (Alt+Tab, otra ventana encima...). Solo se
     * captura el ratón mientras está activa, para no dejar al usuario sin cursor con el juego detrás.
     */
    public static void setFocused(boolean isFocused) {
        if (isFocused && !focused) {
            ignoreButtonsUntilReleased = true;
        }
        focused = isFocused;
        applyCursorCapture();
    }

    /**
     * Suelta el ratón ({@code true}, p.ej. en pausa: se ve el cursor del sistema y se puede sacar de la ventana)
     * o lo vuelve a capturar ({@code false}).
     */
    public static void setCursorReleased(boolean release) {
        released = release;
        applyCursorCapture();
    }

    /** El juego lleva el cursor (capturado y dibujado por el HUD); si no, se ve el del sistema. */
    public static boolean isCursorCaptured() {
        return captured;
    }

    private static void applyCursorCapture() {
        boolean capture = focused && !released;
        if (Gdx.input == null || capture == captured) {
            return;
        }
        if (!capture && focused) {
            // El cursor del sistema aparece donde estaba la mira del juego
            Gdx.input.setCursorPosition(Math.round(mouseX), Math.round(mouseY));
        }
        Gdx.input.setCursorCatched(capture);
        captured = capture;
    }

    /** Lee el estado de este frame. */
    public static void update() {
        System.arraycopy(keys, 0, previousKeys, 0, keys.length);
        System.arraycopy(buttons, 0, previousButtons, 0, buttons.length);
        for (int key = 0; key < keys.length; key++) {
            keys[key] = focused && Gdx.input.isKeyPressed(key);
        }
        updateButtons();
        if (captured) {
            mouseX = clamp(mouseX + Gdx.input.getDeltaX(), DisplayManager.WIDTH);
            mouseY = clamp(mouseY + Gdx.input.getDeltaY(), DisplayManager.HEIGHT);
        } else if (focused) {
            // Ratón suelto: se sigue al cursor del sistema (y al volver a capturar, la mira sigue desde ahí)
            mouseX = clamp(Gdx.input.getX(), DisplayManager.WIDTH);
            mouseY = clamp(Gdx.input.getY(), DisplayManager.HEIGHT);
        }
        scroll = focused ? pendingScroll : 0;
        pendingScroll = 0;
    }

    private static void updateButtons() {
        boolean anyDown = false;
        for (int button = 0; button < buttons.length; button++) {
            anyDown |= Gdx.input.isButtonPressed(button);
        }
        if (!anyDown) {
            ignoreButtonsUntilReleased = false;
        }
        boolean listen = focused && !ignoreButtonsUntilReleased;
        for (int button = 0; button < buttons.length; button++) {
            buttons[button] = listen && Gdx.input.isButtonPressed(button);
        }
    }

    private static float clamp(float value, float max) {
        return Math.max(0, Math.min(max, value));
    }

    /** La tecla está pulsada (p.ej. {@code Keys.W}). */
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

    /** Posición del cursor en pantalla (origen arriba a la izquierda). */
    public static float mouseX() {
        return mouseX;
    }

    public static float mouseY() {
        return mouseY;
    }

    /** Giro de la rueda en este frame: positivo hacia el jugador (abajo), negativo hacia delante, 0 si nada. */
    public static float scroll() {
        return scroll;
    }
}
