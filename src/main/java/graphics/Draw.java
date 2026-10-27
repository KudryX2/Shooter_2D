package graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;

/**
 * Herramientas de dibujo comunes a todos los dibujantes: sprites con {@link SpriteBatch} y formas con
 * {@link ShapeRenderer}. Cambia de uno a otro automáticamente (no pueden estar activos a la vez).
 *
 * <p>La cámara tiene el origen arriba a la izquierda y la y hacia abajo, en píxeles de pantalla. Los ángulos de
 * giro van en grados, en sentido horario en pantalla (como los de {@link util.Geometry}).
 */
final class Draw {
    private static OrthographicCamera camera;
    private static SpriteBatch batch;
    private static ShapeRenderer shapes;
    private static boolean additive;

    private Draw() {
    }

    static void init() {
        camera = new OrthographicCamera();
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        resize(DisplayManager.WIDTH, DisplayManager.HEIGHT);
    }

    static void resize(int width, int height) {
        camera.setToOrtho(true, width, height); // y hacia abajo
    }

    /** Empieza un frame: limpia la pantalla. */
    static void begin() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);
        tint(1, 1, 1, 1);
        setAdditive(false);
    }

    /** Termina el frame: manda a la pantalla lo que quede pendiente. */
    static void end() {
        if (batch.isDrawing()) batch.end();
        if (shapes.isDrawing()) shapes.end();
    }

    static void dispose() {
        batch.dispose();
        shapes.dispose();
    }

    // ---------------------------------------------------------------------------------------------
    // Estado
    // ---------------------------------------------------------------------------------------------

    /** Color por el que se multiplican los sprites siguientes (1, 1, 1, 1 = sin tinte). */
    static void tint(float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
    }

    /**
     * Mezcla aditiva (aclara lo de debajo, para brillos y destellos) o normal. Afecta a sprites y formas
     * siguientes.
     */
    static void setAdditive(boolean on) {
        if (shapes.isDrawing()) {
            shapes.flush();
        }
        additive = on;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, on ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (shapes.isDrawing()) {
            applyBlending();
        }
    }

    private static void applyBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, additive ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ---------------------------------------------------------------------------------------------
    // Sprites
    // ---------------------------------------------------------------------------------------------

    static void sprite(TextureId id, float x, float y, float width, float height) {
        sprite(id, x, y, width, height, 0);
    }

    /** Sprite con la esquina superior izquierda en (x, y), girado {@code rotation} grados sobre su centro. */
    static void sprite(TextureId id, float x, float y, float width, float height, float rotation) {
        TextureRegion region = TexturesManager.region(id);
        sprites().draw(region, x, y, width / 2, height / 2, width, height, 1, 1, rotation);
    }

    /**
     * Dibuja solo un recorte de la imagen: la fracción {@code cropU} de su ancho y {@code cropV} de su alto,
     * desde la esquina superior izquierda (p.ej. una tira para un muro fino, sin aplastar la textura).
     */
    static void spriteCropped(TextureId id, float x, float y, float width, float height, float cropU, float cropV) {
        sprites().draw(TexturesManager.region(id).getTexture(), x, y, width, height, 0, 0, cropU, cropV);
    }

    private static SpriteBatch sprites() {
        if (shapes.isDrawing()) shapes.end();
        if (!batch.isDrawing()) batch.begin();
        return batch;
    }

    // ---------------------------------------------------------------------------------------------
    // Formas
    // ---------------------------------------------------------------------------------------------

    /** Para dibujar formas rellenas (rectángulos, triángulos, elipses, líneas gruesas), con transparencia. */
    static ShapeRenderer shapes() {
        if (batch.isDrawing()) batch.end();
        if (!shapes.isDrawing()) {
            applyBlending(); // SpriteBatch desactiva la mezcla al terminar
            shapes.begin(ShapeRenderer.ShapeType.Filled);
        }
        return shapes;
    }
}
