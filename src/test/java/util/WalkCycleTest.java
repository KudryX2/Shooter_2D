package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalkCycleTest {

    @BeforeEach
    void resetClock() {
        GameClock.reset();
        GameClock.tick(1000);
    }

    @Test
    void walksWhileItKeepsSteppingAndStopsShortlyAfter() {
        WalkCycle walk = new WalkCycle();
        assertFalse(walk.isWalking(), "recién creado está quieto");

        walk.step(5);
        walk.step(3);
        assertTrue(walk.isWalking());
        assertEquals(8, walk.distance());

        GameClock.tick(50);
        assertTrue(walk.isWalking(), "un frame sin moverse no corta la animación");
        GameClock.tick(100);
        assertFalse(walk.isWalking());
    }

    @Test
    void bumpingIntoAWallIsNotAStep() {
        WalkCycle walk = new WalkCycle();
        walk.step(0);
        assertFalse(walk.isWalking());
        assertEquals(0, walk.distance());
    }

    @Test
    void inPauseItStaysAsItWas() {
        WalkCycle walk = new WalkCycle();
        walk.step(4);
        GameClock.togglePause();
        GameClock.tick(1000);
        assertTrue(walk.isWalking(), "congelado a mitad de paso");
        GameClock.togglePause();
    }
}
