package graphics;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Mouse;

import util.GameClock;

/** Botón de pausa en la esquina superior derecha: ⏸ mientras se juega y ▶ en pausa. */
public final class PauseButton {
    private static final float SIZE = 40;
    private static final float MARGIN = 16;

    private static boolean wasPressed;

    private PauseButton() {
    }

    private static float left() {
        return DisplayManager.WIDTH - MARGIN - SIZE;
    }

    private static float top() {
        return MARGIN;
    }

    /** {@code true} si (x, y), en coordenadas de pantalla con el origen arriba a la izquierda, cae en el botón. */
    static boolean contains(float x, float y) {
        return x >= left() && x <= left() + SIZE && y >= top() && y <= top() + SIZE;
    }

    /** El cursor está encima del botón (los clics sobre él no deben disparar). */
    public static boolean isHovered() {
        return contains(Mouse.getX(), DisplayManager.HEIGHT - Mouse.getY());
    }

    /** Llamar una vez por frame: alterna la pausa al hacer clic izquierdo sobre el botón. */
    public static void update() {
        boolean pressed = Mouse.isButtonDown(0);
        if (pressed && !wasPressed && isHovered()) {
            GameClock.togglePause();
        }
        wasPressed = pressed;
    }

    static void draw() {
        float x = left(), y = top();
        boolean hovered = isHovered();

        glDisable(GL_TEXTURE_2D);
        // Fondo
        glColor4f(0, 0, 0, hovered ? 0.65f : 0.4f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + SIZE, y);
        glVertex2f(x + SIZE, y + SIZE);
        glVertex2f(x, y + SIZE);
        glEnd();

        // Icono
        glColor4f(1, 1, 1, hovered ? 1f : 0.85f);
        if (GameClock.isPaused()) {
            // ▶ reanudar
            glBegin(GL_TRIANGLES);
            glVertex2f(x + SIZE * 0.36f, y + SIZE * 0.25f);
            glVertex2f(x + SIZE * 0.36f, y + SIZE * 0.75f);
            glVertex2f(x + SIZE * 0.74f, y + SIZE * 0.5f);
            glEnd();
        } else {
            // ⏸ pausar
            glBegin(GL_QUADS);
            bar(x + SIZE * 0.3f, y + SIZE * 0.25f, SIZE * 0.14f, SIZE * 0.5f);
            bar(x + SIZE * 0.56f, y + SIZE * 0.25f, SIZE * 0.14f, SIZE * 0.5f);
            glEnd();
        }
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }

    private static void bar(float x, float y, float w, float h) {
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
    }
}
