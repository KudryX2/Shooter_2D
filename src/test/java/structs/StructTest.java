package structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import assets.TextureId;

class StructTest {
    private static final float S = Struct.SIZE;

    @Test
    void wallsAreAQuarterAndHalfWallsThreeSixteenthsOfATile() {
        Struct wall = Struct.northWall(0, 0, StructKind.WALL);
        assertEquals(16, wall.halfHeight() * 2);
        assertEquals(S + 16, wall.halfWidth() * 2);

        Struct half = Struct.westWall(0, 0, StructKind.HALF_WALL);
        assertEquals(12, half.halfWidth() * 2);
        assertEquals(S + 12, half.halfHeight() * 2);

        Struct floor = new Struct(0, 0, StructKind.FLOOR, TextureId.GRASS);
        assertEquals(S, floor.halfWidth() * 2);
        assertEquals(S, floor.halfHeight() * 2);
    }

    @Test
    void wallsSitOnTheEdgesOfTheirTile() {
        Struct north = Struct.northWall(3, 5, StructKind.WALL);
        assertEquals(3 * S, north.x());
        assertEquals(5 * S - S / 2, north.y());
        assertFalse(north.vertical());

        Struct west = Struct.westWall(3, 5, StructKind.WALL);
        assertEquals(3 * S - S / 2, west.x());
        assertEquals(5 * S, west.y());
        assertTrue(west.vertical());
    }

    @Test
    void cornersAndStraightJoinsLeaveNoGap() {
        Struct north = Struct.northWall(0, 0, StructKind.WALL);
        Struct west = Struct.westWall(0, 0, StructKind.WALL);
        Struct nextNorth = Struct.northWall(1, 0, StructKind.WALL);
        // El punto de la esquina (-S/2, -S/2) queda dentro de los dos muros
        assertTrue(north.overlaps(-S / 2, -S / 2, 0, 0));
        assertTrue(west.overlaps(-S / 2, -S / 2, 0, 0));
        // Dos tramos seguidos comparten la junta
        assertTrue(north.overlaps(S / 2, -S / 2, 0, 0));
        assertTrue(nextNorth.overlaps(S / 2, -S / 2, 0, 0));
    }

    @Test
    void wallsSnapToTheClosestEdge() {
        // Cerca de la línea horizontal y = S/2 (entre las filas 0 y 1)
        Struct horizontal = Struct.snapped(10, S / 2 - 5, StructKind.WALL, TextureId.WALL);
        assertEquals(Struct.northWall(0, 1, StructKind.WALL), horizontal);

        // Cerca de la línea vertical x = S/2 (entre las columnas 0 y 1)
        Struct vertical = Struct.snapped(S / 2 + 4, -12, StructKind.WALL, TextureId.WALL);
        assertEquals(Struct.westWall(1, 0, StructKind.WALL), vertical);

        // Coordenadas negativas
        Struct negative = Struct.snapped(-2 * S, -S / 2 + 3, StructKind.WALL, TextureId.WALL);
        assertEquals(Struct.northWall(-2, 0, StructKind.WALL), negative);
    }

    @Test
    void floorsSnapToTheTileUnderTheCursor() {
        Struct floor = Struct.snapped(S + 20, 2 * S - 20, StructKind.FLOOR, TextureId.GRASS);
        assertEquals(S, floor.x());
        assertEquals(2 * S, floor.y());
    }
}
