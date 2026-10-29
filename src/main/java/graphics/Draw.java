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
    /** Mundo: alejada {@link DisplayManager#WORLD_ZOOM} alrededor del centro de la pantalla (el jugador). */
    private static OrthographicCamera worldCamera;
    /** HUD: un píxel de pantalla por unidad, sin zoom. */
    private static OrthographicCamera hudCamera;
    private static SpriteBatch batch;
    private static ShapeRenderer shapes;
    private static boolean additive;

    private Draw() {
    }

    static void init() {
        worldCamera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        resize(DisplayManager.WIDTH, DisplayManager.HEIGHT);
    }

    static void resize(int width, int height) {
        setUpWorldCamera(worldCamera, width, height);
        hudCamera.setToOrtho(true, width, height); // y hacia abajo
        hudCamera.update();
    }

    /**
     * Cámara del mundo: y hacia abajo y alejada {@link DisplayManager#WORLD_ZOOM} alrededor del centro de la
     * pantalla, que es donde está el jugador.
     */
    static void setUpWorldCamera(OrthographicCamera camera, int width, int height) {
        camera.setToOrtho(true, width, height);
        camera.zoom = DisplayManager.WORLD_ZOOM;
        // setToOrtho coloca la cámara en (ancho/2, alto/2) multiplicado por el zoom que tuviera antes:
        // se vuelve a centrar a mano para que el jugador quede siempre en el medio
        camera.position.set(width / 2f, height / 2f, 0);
        camera.update();
    }

    /** Empieza un frame: limpia la pantalla y dibuja el mundo hasta que se pida {@link #hudView()}. */
    static void begin() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        tint(1, 1, 1, 1);
        setAdditive(false);
        worldView();
    }

    /** Lo siguiente se dibuja en el mundo (con zoom). */
    static void worldView() {
        use(worldCamera);
    }

    /** Lo siguiente se dibuja en el HUD (en píxeles de pantalla). */
    static void hudView() {
        use(hudCamera);
    }

    private static void use(OrthographicCamera camera) {
        end(); // lo pendiente se dibuja con la cámara anterior
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);
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
        region(TexturesManager.region(id), x, y, width, height, rotation);
    }

    /** Una región concreta del atlas (p.ej. una variante de {@link TexturesManager#variant}). */
    static void region(TextureRegion region, float x, float y, float width, float height, float rotation) {
        sprites().draw(region, x, y, width / 2, height / 2, width, height, 1, 1, rotation);
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
