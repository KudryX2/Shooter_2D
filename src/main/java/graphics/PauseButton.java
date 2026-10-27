package graphics;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import input.Input;
import util.GameClock;

/** Botón de pausa en la esquina superior derecha: ⏸ mientras se juega y ▶ en pausa. */
public final class PauseButton {
    private static final float SIZE = 40;
    private static final float MARGIN = 16;

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
        return contains(Input.mouseX(), Input.mouseY());
    }

    /** Llamar una vez por frame: alterna la pausa al hacer clic izquierdo sobre el botón. */
    public static void update() {
        if (Input.wasButtonPressed(0) && isHovered()) {
            GameClock.togglePause();
        }
    }

    static void draw() {
        float x = left(), y = top();
        boolean hovered = isHovered();
        ShapeRenderer shapes = Draw.shapes();

        // Fondo
        shapes.setColor(0, 0, 0, hovered ? 0.65f : 0.4f);
        shapes.rect(x, y, SIZE, SIZE);

        // Icono
        shapes.setColor(1, 1, 1, hovered ? 1f : 0.85f);
        if (GameClock.isPaused()) {
            // ▶ reanudar
            shapes.triangle(x + SIZE * 0.36f, y + SIZE * 0.25f,
                    x + SIZE * 0.36f, y + SIZE * 0.75f,
                    x + SIZE * 0.74f, y + SIZE * 0.5f);
        } else {
            // ⏸ pausar
            shapes.rect(x + SIZE * 0.3f, y + SIZE * 0.25f, SIZE * 0.14f, SIZE * 0.5f);
            shapes.rect(x + SIZE * 0.56f, y + SIZE * 0.25f, SIZE * 0.14f, SIZE * 0.5f);
        }
    }
}
