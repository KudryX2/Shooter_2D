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
 * <p>Las teclas son las de libGDX ({@link Keys}); los botones del ratón, 0 = izquierdo y 1 = derecho. El cursor
 * del sistema está capturado: el juego lleva su propio cursor, que no sale de la ventana. Sus coordenadas son
 * de pantalla, con el origen arriba a la izquierda.
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

    private Input() {
    }

    /** Captura el cursor y empieza a escuchar la rueda. Requiere la ventana creada. */
    public static void init() {
        Gdx.input.setCursorCatched(true);
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

    /** Lee el estado de este frame. */
    public static void update() {
        System.arraycopy(keys, 0, previousKeys, 0, keys.length);
        System.arraycopy(buttons, 0, previousButtons, 0, buttons.length);
        for (int key = 0; key < keys.length; key++) {
            keys[key] = Gdx.input.isKeyPressed(key);
        }
        for (int button = 0; button < buttons.length; button++) {
            buttons[button] = Gdx.input.isButtonPressed(button);
        }
        mouseX = clamp(mouseX + Gdx.input.getDeltaX(), DisplayManager.WIDTH);
        mouseY = clamp(mouseY + Gdx.input.getDeltaY(), DisplayManager.HEIGHT);
        scroll = pendingScroll;
        pendingScroll = 0;
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
