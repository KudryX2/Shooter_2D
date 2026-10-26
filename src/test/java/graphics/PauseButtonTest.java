package graphics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PauseButtonTest {

    @Test
    void buttonSitsInTheTopRightCorner() {
        float w = DisplayManager.WIDTH;
        assertTrue(PauseButton.contains(w - 36, 36), "centro del botón");
        assertTrue(PauseButton.contains(w - 56, 16), "esquina superior izquierda del botón");
        assertFalse(PauseButton.contains(w - 60, 36), "a la izquierda del botón");
        assertFalse(PauseButton.contains(w - 36, 60), "debajo del botón");
        assertFalse(PauseButton.contains(w / 2, DisplayManager.HEIGHT / 2f), "centro de la pantalla");
    }
}
