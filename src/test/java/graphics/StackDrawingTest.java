package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StackDrawingTest {

    @Test
    void biggerPilesShowMoreCopies() {
        assertEquals(1, StackDrawing.copies(1));
        assertEquals(2, StackDrawing.copies(2));
        assertEquals(2, StackDrawing.copies(4));
        assertEquals(3, StackDrawing.copies(5));
        assertEquals(3, StackDrawing.copies(20));
    }
}
