package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import assets.TextureId;

class FacingTest {

    @Test
    void anglesGoClockwiseFromTheRight() {
        assertEquals(Facing.RIGHT, Facing.of(0));
        assertEquals(Facing.DOWN, Facing.of(90));
        assertEquals(Facing.LEFT, Facing.of(180));
        assertEquals(Facing.UP, Facing.of(270));
    }

    @Test
    void diagonalsAndOutOfRangeAngles() {
        assertEquals(Facing.RIGHT, Facing.of(44));
        assertEquals(Facing.DOWN, Facing.of(45));
        assertEquals(Facing.UP, Facing.of(-90));
        assertEquals(Facing.RIGHT, Facing.of(-30));
        assertEquals(Facing.LEFT, Facing.of(180 + 360 * 3));
    }

    @Test
    void theOrderMatchesTheImageNames() {
        for (Facing facing : Facing.values()) {
            assertEquals(facing.name().toLowerCase(), TextureId.DIRECTIONS[facing.ordinal()]);
        }
    }
}
