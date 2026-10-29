package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Las cuentas del modo {@link DisplayManager#PIXEL_PERFECT} (sin ventana: solo la geometría). */
class PixelPerfectTest {
    private static final float ART = DisplayManager.ART_PIXEL;

    @AfterEach
    void restoreWindowSize() {
        DisplayManager.resize(DisplayManager.WINDOWED_WIDTH, DisplayManager.WINDOWED_HEIGHT);
    }

    @Test
    void theScaleIsAWholeNumberCloseToTheOldZoom() {
        DisplayManager.resize(1600, 900);
        assertEquals(3, DisplayManager.pixelScale());
        if (DisplayManager.PIXEL_PERFECT) {
            assertEquals(ART / 3, DisplayManager.worldZoom(), 1e-6f);
        }

        DisplayManager.resize(1920, 1080);
        assertEquals(4, DisplayManager.pixelScale());
        DisplayManager.resize(800, 450);
        assertEquals(2, DisplayManager.pixelScale());
        DisplayManager.resize(0, 0); // ventana minimizada
        assertEquals(1, DisplayManager.pixelScale());
    }

    @Test
    void theBufferIsEvenAndCoversTheScreenWithAMargin() {
        for (int screen : new int[] {900, 1600, 1601, 1080, 0}) {
            int size = Draw.bufferSize(screen, 3);
            assertEquals(0, size % 2, "par: " + size);
            assertTrue(size * 3 >= screen + 2 * 3, "cubre la pantalla con margen: " + size);
        }
    }

    @Test
    void offsetInsideThePixelWorksForNegativeCoordinatesToo() {
        assertEquals(0, Camera.offsetInPixel(8), 1e-6f);
        assertEquals(1.5f, Camera.offsetInPixel(9.5f), 1e-6f);
        assertEquals(3, Camera.offsetInPixel(-1), 1e-6f);
        assertEquals(0, Camera.offsetInPixel(-8), 1e-6f);
    }

    /**
     * Un punto del mundo acaba en el mismo sitio de la pantalla que si se dibujara directamente con zoom (a menos
     * de un píxel): la imagen intermedia no mueve nada, solo alinea los píxeles.
     */
    @Test
    void goingThroughTheBufferKeepsEverythingWhereTheZoomWouldPutIt() {
        int width = 1600, height = 900, scale = 3;
        float zoom = ART / scale;
        int bufferWidth = Draw.bufferSize(width, scale);
        for (float playerX : new float[] {0, 1, 2.5f, 3.9f, 517.25f, -6.3f}) {
            float offset = Camera.offsetInPixel(playerX);
            // La cámara del buffer está centrada en W/2 - offset y mide bufferWidth píxeles de ART unidades
            float bufferLeft = width / 2f - offset - bufferWidth * ART / 2;
            float start = Draw.bufferScreenStart(width, bufferWidth, offset, scale);
            for (float u : new float[] {width / 2f, width / 2f + 100, width / 2f - 333}) {
                float inBuffer = (u - bufferLeft) / ART; // en píxeles del dibujo
                float onScreen = start + inBuffer * scale;
                float expected = width / 2f + (u - width / 2f) / zoom;
                assertEquals(expected, onScreen, 1, "jugador en x=" + playerX + ", punto " + u);
            }
        }
    }
}
