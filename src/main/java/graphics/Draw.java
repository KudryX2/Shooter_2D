package graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;

/**
 * Herramientas de dibujo comunes a todos los dibujantes: sprites con {@link SpriteBatch} y formas con
 * {@link ShapeRenderer}. Cambia de uno a otro automáticamente (no pueden estar activos a la vez).
 *
 * <p>La cámara tiene el origen arriba a la izquierda y la y hacia abajo. El mundo se dibuja en "coordenadas de
 * pantalla sin zoom" ({@link Camera}) y el HUD en píxeles de pantalla. Los ángulos de giro van en grados, en sentido
 * horario en pantalla (como los de {@link util.Geometry}).
 *
 * <p>Con {@link DisplayManager#PIXEL_PERFECT} el mundo no se dibuja directamente en la pantalla, sino en una imagen
 * pequeña ({@link #worldBuffer}) con un píxel por cada píxel del pixel art, alineada a la cuadrícula de píxeles del
 * mundo. Al pasar al HUD esa imagen se amplía a la pantalla {@link DisplayManager#pixelScale()} veces, sin suavizar:
 * todos los píxeles del dibujo salen del mismo tamaño y no tiemblan al moverse. Lo que se mueve el jugador por
 * debajo de un píxel del dibujo se compensa desplazando la imagen al ampliarla, así que la cámara va suave.
 */
final class Draw {
    /** Mundo sin {@link DisplayManager#PIXEL_PERFECT}: con zoom alrededor del centro de la pantalla (el jugador). */
    private static OrthographicCamera worldCamera;
    /** Mundo con {@link DisplayManager#PIXEL_PERFECT}: dibuja en {@link #worldBuffer}. */
    private static OrthographicCamera bufferCamera;
    /** HUD: un píxel de pantalla por unidad, sin zoom. */
    private static OrthographicCamera hudCamera;
    private static SpriteBatch batch;
    private static ShapeRenderer shapes;
    private static boolean additive;

    private static FrameBuffer worldBuffer;
    private static TextureRegion worldBufferRegion;
    /** Tamaño de {@link #worldBuffer} en píxeles del dibujo (pares, para que su centro caiga entre dos píxeles). */
    private static int bufferWidth, bufferHeight;
    /** {@link #worldBuffer} está activo: lo que se dibuja va a él y no a la pantalla. */
    private static boolean drawingToBuffer;

    private Draw() {
    }

    static void init() {
        worldCamera = new OrthographicCamera();
        bufferCamera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        resize(DisplayManager.WIDTH, DisplayManager.HEIGHT);
    }

    static void resize(int width, int height) {
        setUpWorldCamera(worldCamera, width, height);
        hudCamera.setToOrtho(true, width, height); // y hacia abajo
        hudCamera.update();
        if (DisplayManager.PIXEL_PERFECT) {
            createWorldBuffer(width, height);
        }
    }

    /**
     * Cámara del mundo sin {@link DisplayManager#PIXEL_PERFECT}: y hacia abajo y alejada
     * {@link DisplayManager#worldZoom()} alrededor del centro de la pantalla, que es donde está el jugador.
     */
    static void setUpWorldCamera(OrthographicCamera camera, int width, int height) {
        camera.setToOrtho(true, width, height);
        camera.zoom = DisplayManager.worldZoom();
        // setToOrtho coloca la cámara en (ancho/2, alto/2) multiplicado por el zoom que tuviera antes:
        // se vuelve a centrar a mano para que el jugador quede siempre en el medio
        camera.position.set(width / 2f, height / 2f, 0);
        camera.update();
    }

    /** Lo bastante grande para cubrir la pantalla ampliado, con un píxel de margen por cada lado para desplazarlo. */
    private static void createWorldBuffer(int width, int height) {
        if (worldBuffer != null) {
            worldBuffer.dispose();
        }
        int scale = DisplayManager.pixelScale();
        bufferWidth = bufferSize(width, scale);
        bufferHeight = bufferSize(height, scale);
        worldBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, bufferWidth, bufferHeight, false);
        worldBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        // Sin voltear: la imagen queda con su parte de arriba en v = 1, que es lo que pinta arriba una cámara con
        // la y hacia abajo
        worldBufferRegion = new TextureRegion(worldBuffer.getColorBufferTexture());
        float art = DisplayManager.ART_PIXEL;
        bufferCamera.setToOrtho(true, bufferWidth * art, bufferHeight * art);
    }

    /** Píxeles del dibujo que hacen falta para cubrir {@code screenPixels} ampliados, más margen, redondeado a par. */
    static int bufferSize(int screenPixels, int scale) {
        int size = (int) Math.ceil(screenPixels / (double) scale) + 2;
        return size + (size % 2);
    }

    /** Empieza un frame: limpia la pantalla y dibuja el mundo hasta que se pida {@link #hudView()}. */
    static void begin() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        tint(1, 1, 1, 1);
        setAdditive(false);
        if (DisplayManager.PIXEL_PERFECT) {
            worldBuffer.begin();
            drawingToBuffer = true;
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            // Centrada en el píxel del dibujo donde está el jugador: lo que le sobre se compensa al ampliarla
            bufferCamera.position.set(DisplayManager.WIDTH / 2f - Camera.offsetInPixelX(),
                    DisplayManager.HEIGHT / 2f - Camera.offsetInPixelY(), 0);
            bufferCamera.update();
            use(bufferCamera);
        } else {
            use(worldCamera);
        }
    }

    /** Lo siguiente se dibuja en el HUD (en píxeles de pantalla). Con pixel art, antes se pasa el mundo a pantalla. */
    static void hudView() {
        flush();
        finishWorldBuffer();
        use(hudCamera);
    }

    /** Amplía {@link #worldBuffer} a la pantalla, desplazado lo que el jugador se ha movido dentro de su píxel. */
    private static void finishWorldBuffer() {
        if (!drawingToBuffer) {
            return;
        }
        worldBuffer.end();
        drawingToBuffer = false;

        int scale = DisplayManager.pixelScale();
        float x = bufferScreenStart(DisplayManager.WIDTH, bufferWidth, Camera.offsetInPixelX(), scale);
        float y = bufferScreenStart(DisplayManager.HEIGHT, bufferHeight, Camera.offsetInPixelY(), scale);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.setColor(1, 1, 1, 1);
        batch.disableBlending(); // la imagen ya trae todo mezclado: su transparencia no debe aclarar ni oscurecer
        batch.begin();
        batch.draw(worldBufferRegion, x, y, bufferWidth * scale, bufferHeight * scale);
        batch.end();
        batch.enableBlending();
    }

    /**
     * Dónde empieza en pantalla (en un eje) el mundo ampliado: centrado, y desplazado hacia atrás lo que el jugador se
     * ha metido en su píxel ({@code offset}, en unidades del mundo), redondeado a píxeles de pantalla.
     */
    static float bufferScreenStart(int screenSize, int bufferSize, float offset, int scale) {
        float toScreen = scale / DisplayManager.ART_PIXEL;
        return Math.round(screenSize / 2f - offset * toScreen - bufferSize * scale / 2f);
    }

    private static void use(OrthographicCamera camera) {
        flush(); // lo pendiente se dibuja con la cámara anterior
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);
    }

    /** Manda a dibujar lo que haya pendiente. */
    private static void flush() {
        if (batch.isDrawing()) batch.end();
        if (shapes.isDrawing()) shapes.end();
    }

    /** Termina el frame: manda a la pantalla lo que quede pendiente. */
    static void end() {
        flush();
        finishWorldBuffer();
    }

    static void dispose() {
        batch.dispose();
        shapes.dispose();
        if (worldBuffer != null) {
            worldBuffer.dispose();
        }
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

    /**
     * Como {@link #sprite(TextureId, float, float, float, float, float)}, pero volteado de arriba abajo antes de
     * girarlo si {@code flipped} (p.ej. un arma apuntando a la izquierda, para que no quede boca abajo).
     */
    static void sprite(TextureId id, float x, float y, float width, float height, float rotation, boolean flipped) {
        sprites().draw(TexturesManager.region(id), x, y, width / 2, height / 2, width, height, 1, flipped ? -1 : 1,
                rotation);
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
