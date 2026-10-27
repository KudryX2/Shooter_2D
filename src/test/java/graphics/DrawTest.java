package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;

class DrawTest {
    private static final int W = 1600, H = 900;

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load(); // las matrices de libGDX usan código nativo (no hace falta ventana)
    }

    /** Dónde acaba en la ventana (píxeles, origen arriba a la izquierda) un punto dibujado en (x, y). */
    private static Vector3 onScreen(OrthographicCamera camera, float x, float y) {
        Vector3 p = new Vector3(x, y, 0).prj(camera.combined); // de -1 a 1, y hacia arriba
        return new Vector3((p.x + 1) / 2 * W, (1 - p.y) / 2 * H, 0);
    }

    @Test
    void thePlayerStaysInTheMiddleOfTheWindowAfterRepeatedResizes() {
        OrthographicCamera camera = new OrthographicCamera();
        for (int i = 0; i < 3; i++) { // libGDX llama a resize más de una vez
            Draw.setUpWorldCamera(camera, W, H);
        }
        Vector3 player = onScreen(camera, W / 2f, H / 2f);
        assertEquals(W / 2f, player.x, 0.01f);
        assertEquals(H / 2f, player.y, 0.01f);
    }

    @Test
    void theWorldIsZoomedOutAroundThePlayerWithYDown() {
        OrthographicCamera camera = new OrthographicCamera();
        Draw.setUpWorldCamera(camera, W, H);
        // 100 unidades a la derecha y abajo del jugador se ven a 100 / zoom píxeles
        Vector3 p = onScreen(camera, W / 2f + 100, H / 2f + 100);
        assertEquals(W / 2f + 100 / DisplayManager.WORLD_ZOOM, p.x, 0.01f);
        assertEquals(H / 2f + 100 / DisplayManager.WORLD_ZOOM, p.y, 0.01f, "y hacia abajo");
    }
}
