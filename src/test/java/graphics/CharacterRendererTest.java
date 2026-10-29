package graphics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterRendererTest {

    /** Apuntando a la izquierda el arma se voltea, para que no quede boca abajo. */
    @Test
    void heldItemsAreFlippedWhenAimingLeft() {
        assertFalse(CharacterRenderer.pointsLeft(0), "derecha");
        assertFalse(CharacterRenderer.pointsLeft(80), "casi abajo, hacia la derecha");
        assertFalse(CharacterRenderer.pointsLeft(-60), "arriba a la derecha");
        assertTrue(CharacterRenderer.pointsLeft(100), "casi abajo, hacia la izquierda");
        assertTrue(CharacterRenderer.pointsLeft(180), "izquierda");
        assertTrue(CharacterRenderer.pointsLeft(250), "arriba a la izquierda");
    }
}
