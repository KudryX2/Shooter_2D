package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import npc.Npc;
import npc.NpcType;
import util.GameClock;

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

    /** Antes de morder se echa atrás (desplazamiento negativo) y al morder se lanza hacia delante. */
    @Test
    void attackersLeanBackThenLunge() {
        GameClock.reset();
        Npc zombie = new Npc(NpcType.ZOMBIE);
        assertEquals(0, CharacterRenderer.attackLean(zombie), 0.01f, "sin atacar");
        zombie.setAttackCharge(1);
        assertTrue(CharacterRenderer.attackLean(zombie) < 0, "preparando el mordisco");
        zombie.markAttack();
        assertTrue(CharacterRenderer.attackLean(zombie) > 0, "lanzándose");
        GameClock.tick(500);
        assertEquals(0, CharacterRenderer.attackLean(zombie), 0.01f, "ya ha vuelto");
    }
}
