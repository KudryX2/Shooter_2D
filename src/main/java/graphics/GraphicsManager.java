package graphics;

import static graphics.Sprite.drawSprite;
import static org.lwjgl.opengl.GL11.*;
import static util.Geometry.distance;

import java.util.List;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.opengl.Texture;

import assets.TextureId;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.Item;
import items.Projectile;
import items.StructItem;
import npc.Npc;
import npc.NpcKind;
import player.Player;
import structs.Struct;
import util.GameClock;
import world.Chunk;
import world.MuzzleFlash;
import world.WorldManager;

public final class GraphicsManager {
    private static final int CELL_SPACING = 50;
    /** Tamaño en pantalla del sprite del jugador (también lo usan los NPC humanos). */
    private static final float CHARACTER_SIZE = 100;
    /**
     * Tamaño en pantalla de zombis y cadáveres. El jugador solo ocupa ~44% de su imagen (28 de 64 px) y los
     * zombis ~72% (22-24 de 32 px), así que con 62 px se ven del mismo tamaño que el jugador a 100 px (~44 px).
     */
    private static final float ZOMBIE_SIZE = 62;
    /** Charco bajo un humano abatido mientras se convierte en zombi. */
    private static final float BLOOD_PUDDLE_SIZE = 110;

    // Sombras
    private static final float SHADOW_ALPHA = 0.3f;
    private static final float WALL_SHADOW_OFFSET = 8;
    private static final float HALF_WALL_SHADOW_OFFSET = 4;
    private static final float CHARACTER_SHADOW_OFFSET = 4;
    /** Radio de la sombra de jugador y NPC; el cuerpo visible mide ~22 px de radio. */
    private static final float CHARACTER_SHADOW_RADIUS = 16;
    private static final int SHADOW_SEGMENTS = 16;

    // Efectos de disparo
    private static final float TRAIL_LENGTH = 22;
    private static final float TRAIL_WIDTH = 1.5f;
    private static final float FLASH_FORWARD = 14;
    private static final float FLASH_LENGTH = 22;
    private static final float FLASH_WIDTH = 6;
    private static final long HIT_FLASH_MS = 100;
    private static final float HAND_OFFSET = 40;

    /** Escala del HUD (teclas N/M). */
    public static float hudScale;

    private GraphicsManager() {
    }

    public static void init() {
        hudScale = DisplayManager.WIDTH / 100f;
    }

    /** Orden de capas, de abajo arriba: suelo y muros, objetos tirados, NPC, jugador, fogonazos y HUD. */
    public static void render() {
        drawEnvironment();
        drawDroppedItems();
        drawNpcs();
        drawPlayer();
        drawMuzzleFlashes();
        drawHud();
    }

    private static float screenX(float worldX) {
        return worldX - Player.x + DisplayManager.WIDTH / 2f;
    }

    private static float screenY(float worldY) {
        return worldY - Player.y + DisplayManager.HEIGHT / 2f;
    }

    private static boolean inRenderDistance(float worldX, float worldY) {
        return distance(worldX, worldY, Player.x, Player.y) < DisplayManager.WIDTH;
    }

    private static void drawEnvironment() {
        // Orden de dibujado: suelos -> sombras de muros -> proyectiles -> muros -> medios muros
        for (Chunk chunk : WorldManager.activeChunks) drawFloors(chunk.floors);
        for (Chunk chunk : WorldManager.activeChunks) {
            drawWallShadows(chunk.walls, WALL_SHADOW_OFFSET);
            drawWallShadows(chunk.halfWalls, HALF_WALL_SHADOW_OFFSET);
        }
        drawProjectiles();
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.walls);
        // Medios muros tintados de color saco terrero: se distinguen de los muros (las balas los atraviesan)
        glColor3f(0.85f, 0.72f, 0.5f);
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.halfWalls);
        glColor3f(1f, 1f, 1f);
    }

    /** Bala con una estela fina que se desvanece hacia atrás (nunca más larga que lo que lleva recorrido). */
    private static void drawProjectiles() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(TRAIL_WIDTH);
        glBegin(GL_LINES);
        for (Projectile p : WorldManager.projectiles) {
            if (!inRenderDistance(p.x, p.y)) {
                continue;
            }
            double angle = Math.toRadians(-p.rotation + 90);
            float dx = (float) Math.sin(angle);
            float dy = (float) Math.cos(angle);
            float length = Math.min(TRAIL_LENGTH, distance(p.x, p.y, p.startX, p.startY));
            float headX = screenX(p.x);
            float headY = screenY(p.y);
            glColor4f(1f, 0.95f, 0.75f, 0.9f);
            glVertex2f(headX, headY);
            glColor4f(1f, 0.75f, 0.35f, 0f);
            glVertex2f(headX - dx * length, headY - dy * length);
        }
        glEnd();
        glDisable(GL_LINE_SMOOTH);
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);

        for (Projectile p : WorldManager.projectiles) {
            if (inRenderDistance(p.x, p.y)) {
                drawSprite(tex(TextureId.BULLET), screenX(p.x) - 2, screenY(p.y) - 2, 4, 4, p.rotation);
            }
        }
    }

    /** Destello en forma de rombo alargado hacia delante, con mezcla aditiva y que se apaga en {@link MuzzleFlash#DURATION_MS}. */
    private static void drawMuzzleFlashes() {
        long now = GameClock.now();
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        for (MuzzleFlash flash : WorldManager.muzzleFlashes) {
            float life = 1 - (now - flash.time()) / (float) MuzzleFlash.DURATION_MS;
            if (life <= 0) {
                continue;
            }
            float x, y, rotation;
            if (flash.byPlayer()) {
                rotation = Player.rotation;
                x = playerHandX();
                y = playerHandY();
            } else {
                if (!inRenderDistance(flash.x(), flash.y())) {
                    continue;
                }
                rotation = flash.rotation();
                x = screenX(flash.x());
                y = screenY(flash.y());
            }
            double angle = Math.toRadians(-rotation + 90);
            float dx = (float) Math.sin(angle);
            float dy = (float) Math.cos(angle);
            float cx = x + dx * FLASH_FORWARD;
            float cy = y + dy * FLASH_FORWARD;
            float length = FLASH_LENGTH * (0.6f + 0.4f * life);

            glBegin(GL_TRIANGLE_FAN);
            glColor4f(1f, 1f, 0.85f, life);
            glVertex2f(cx, cy);
            glColor4f(1f, 0.6f, 0.1f, 0f);
            glVertex2f(cx + dx * length, cy + dy * length);                   // punta
            glVertex2f(cx - dy * FLASH_WIDTH, cy + dx * FLASH_WIDTH);         // lado
            glVertex2f(cx - dx * FLASH_WIDTH * 0.6f, cy - dy * FLASH_WIDTH * 0.6f); // detrás
            glVertex2f(cx + dy * FLASH_WIDTH, cy - dx * FLASH_WIDTH);         // otro lado
            glVertex2f(cx + dx * length, cy + dy * length);                   // cierra en la punta
            glEnd();
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }

    /** Cada casilla gira 0/90/180/270º y cambia un poco de tono según su posición, para que no se note la repetición. */
    private static void drawFloors(List<Struct> floors) {
        float half = Struct.SIZE / 2;
        for (Struct s : floors) {
            if (!inRenderDistance(s.x(), s.y())) {
                continue;
            }
            int hash = tileHash(s.x(), s.y());
            boolean picture = s.texture() == TextureId.TEEMO; // las imágenes no se giran
            float rotation = picture ? 0 : (hash & 3) * 90;
            float tone = picture ? 1 : 0.9f + ((hash >>> 8) & 0xFF) / 255f * 0.1f;
            glColor3f(tone, tone, tone);
            drawSprite(tex(s.texture()),
                    screenX(s.x()) - half, screenY(s.y()) - half, Struct.SIZE, Struct.SIZE, rotation);
        }
        glColor3f(1f, 1f, 1f);
    }

    private static int tileHash(float x, float y) {
        int h = (int) x * 73_856_093 ^ (int) y * 19_349_663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return h ^ (h >>> 15);
    }

    private static void drawStructs(List<Struct> structs) {
        float half = Struct.SIZE / 2;
        for (Struct s : structs) {
            if (inRenderDistance(s.x(), s.y())) {
                drawSprite(tex(s.texture()),
                        screenX(s.x()) - half, screenY(s.y()) - half, Struct.SIZE, Struct.SIZE);
            }
        }
    }

    /** Sombra proyectada hacia abajo a la derecha; como todas tienen el mismo desplazamiento, encajan entre sí. */
    private static void drawWallShadows(List<Struct> structs, float offset) {
        float half = Struct.SIZE / 2;
        glDisable(GL_TEXTURE_2D);
        glColor4f(0, 0, 0, SHADOW_ALPHA);
        glBegin(GL_QUADS);
        for (Struct s : structs) {
            if (inRenderDistance(s.x(), s.y())) {
                float x = screenX(s.x()) - half + offset;
                float y = screenY(s.y()) - half + offset;
                glVertex2f(x, y);
                glVertex2f(x + Struct.SIZE, y);
                glVertex2f(x + Struct.SIZE, y + Struct.SIZE);
                glVertex2f(x, y + Struct.SIZE);
            }
        }
        glEnd();
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }

    /** Sombra elíptica bajo un personaje, centrada en (x, y) de pantalla. */
    private static void drawCharacterShadow(float x, float y, float radius) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0, 0, 0, SHADOW_ALPHA);
        glBegin(GL_TRIANGLE_FAN);
        float cx = x + CHARACTER_SHADOW_OFFSET;
        float cy = y + CHARACTER_SHADOW_OFFSET;
        glVertex2f(cx, cy);
        for (int i = 0; i <= SHADOW_SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SHADOW_SEGMENTS;
            glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius * 0.8f);
        }
        glEnd();
        glColor4f(1, 1, 1, 1);
        glEnable(GL_TEXTURE_2D);
    }

    /**
     * Los NPC se dibujan por capas, sin depender del orden de la lista: así nada del suelo (cadáveres, charcos,
     * sombras) puede quedar encima de un NPC en pie.
     */
    private static void drawNpcs() {
        // 1. Suelo: cadáveres, charcos de los que se están levantando y sombras
        float puddleHalf = BLOOD_PUDDLE_SIZE / 2;
        for (Npc npc : WorldManager.npcs) {
            if (!inRenderDistance(npc.x, npc.y)) {
                continue;
            }
            if (!npc.alive) {
                drawNpcBody(npc); // cadáver: sprite de sangre
            } else if (npc.isRising()) {
                // Giro fijo por NPC para que no todos los charcos sean iguales
                float rotation = Math.floorMod(System.identityHashCode(npc), 360);
                drawSprite(tex(TextureId.BLOOD), screenX(npc.x) - puddleHalf,
                        screenY(npc.y) - puddleHalf, BLOOD_PUDDLE_SIZE, BLOOD_PUDDLE_SIZE, rotation);
            } else {
                drawCharacterShadow(screenX(npc.x), screenY(npc.y), CHARACTER_SHADOW_RADIUS);
            }
        }

        // 2. Abatidos que se están levantando (tumbados en el suelo)
        for (Npc npc : WorldManager.npcs) {
            if (npc.isRising() && inRenderDistance(npc.x, npc.y)) {
                drawNpcBody(npc);
            }
        }

        // 3. NPC en pie, encima de todo lo anterior
        for (Npc npc : WorldManager.npcs) {
            if (npc.alive && !npc.isRising() && inRenderDistance(npc.x, npc.y)) {
                drawNpcBody(npc);
            }
        }

        // 4. Barras de vida, por encima de todos los cuerpos
        for (Npc npc : WorldManager.npcs) {
            if (npc.hp > 0 && npc.hp < 100 && inRenderDistance(npc.x, npc.y)) {
                drawHealthBar(npc.hp, screenX(npc.x) - 25, screenY(npc.y) - spriteSize(npc) / 2 + 5);
            }
        }
    }

    /** Sprite del NPC (con tinte según comportamiento y parpadeo si acaba de recibir un balazo). */
    private static void drawNpcBody(Npc npc) {
        if (npc.kind == NpcKind.HUMAN) {
            // Tinte según el comportamiento para distinguir aliados, civiles y hostiles
            switch (npc.behaviour) {
                case REACTIVE -> glColor3f(0.6f, 1f, 0.6f);
                case PASSIVE -> glColor3f(0.7f, 0.8f, 1f);
                case AGGRESSIVE -> glColor3f(1f, 0.55f, 0.55f);
            }
        }
        // El sprite del jugador mira hacia abajo y el de los zombis hacia arriba
        boolean humanSprite = npc.texture == TextureId.HUMAN;
        float rotation = humanSprite ? npc.rotation + 180 : npc.rotation;
        float size = spriteSize(npc);
        float half = size / 2;
        drawSprite(tex(npc.texture), screenX(npc.x) - half, screenY(npc.y) - half,
                size, size, rotation);
        glColor3f(1f, 1f, 1f);
        if (npc.wasHitWithin(HIT_FLASH_MS)) {
            // Parpadeo al recibir un balazo: se vuelve a dibujar el sprite con mezcla aditiva para aclararlo
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);
            glColor4f(1f, 0.75f, 0.75f, 0.85f);
            drawSprite(tex(npc.texture), screenX(npc.x) - half, screenY(npc.y) - half,
                    size, size, rotation);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1, 1, 1, 1);
        }
    }

    private static float spriteSize(Npc npc) {
        return npc.texture == TextureId.HUMAN ? CHARACTER_SIZE : ZOMBIE_SIZE;
    }

    private static void drawHealthBar(int hp, float x, float y) {
        drawSprite(tex(TextureId.HP_BACKGROUND), x, y, 50, 5);
        TextureId texture = hp > 50 ? TextureId.HP_HIGH : hp > 25 ? TextureId.HP_MEDIUM : TextureId.HP_LOW;
        drawSprite(tex(texture), x, y, hp / 2f, 5);
    }

    private static void drawPlayer() {
        float centerX = DisplayManager.WIDTH / 2f;
        float centerY = DisplayManager.HEIGHT / 2f;

        // El sprite del jugador está girado 90º
        float half = CHARACTER_SIZE / 2;
        drawCharacterShadow(centerX, centerY, CHARACTER_SHADOW_RADIUS);
        drawSprite(tex(TextureId.PLAYER), centerX - half, centerY - half,
                CHARACTER_SIZE, CHARACTER_SIZE, Player.rotation - 90);
        if (Player.hp > 0) {
            drawHealthBar(Player.hp, centerX - Player.SIZE / 2, centerY - Player.SIZE - 5);
        }

        // Objeto en la mano
        float handX = playerHandX();
        float handY = playerHandY();

        switch (Player.selectedItem()) {
            case null -> { }
            // 32 px: antes se dibujaba a 50 pero la imagen solo ocupaba 20/32 del cuadro (relleno)
            case Gun gun -> drawSprite(tex(gun.heldTexture),
                    handX - 16, handY - 16, 32, 32, Player.rotation);
            case Consumable consumable -> drawSprite(tex(consumable.heldTexture),
                    handX - 12, handY - 12, 24, 24, Player.rotation);
            case StructItem struct -> {
                if (Player.buildMode) {
                    drawSprite(tex(struct.heldTexture),
                            handX - 20, handY - 20, 40, 40, Player.rotation);
                }
            }
        }
    }

    /** Posición en pantalla de la mano del jugador (donde se dibuja el arma). */
    private static float playerHandX() {
        return DisplayManager.WIDTH / 2f + (float) Math.sin(Math.toRadians(-Player.rotation + 90)) * HAND_OFFSET;
    }

    private static float playerHandY() {
        return DisplayManager.HEIGHT / 2f + (float) Math.cos(Math.toRadians(-Player.rotation + 90)) * HAND_OFFSET;
    }

    private static void drawHud() {
        float width = DisplayManager.WIDTH;
        float height = DisplayManager.HEIGHT;
        float cellY = height / 1.1f;
        float cellSize = 50 + hudScale;

        // Inventario
        for (int i = 0; i < Player.inventory.size(); i++) {
            float x = width / 2 + 5 + (CELL_SPACING + hudScale) * i;
            drawSprite(tex(Player.inventory.get(i).icon), x, cellY + 5, 40 + hudScale, 40 + hudScale);
        }

        // Casillas de la mano
        for (int i = 0; i < Player.HAND_SLOTS; i++) {
            float x = width / 2 + (CELL_SPACING + hudScale) * i;
            drawSprite(tex(TextureId.NORMAL_CELL), x, cellY, cellSize, cellSize);
            if (i == Player.selectedCell) {
                drawSprite(tex(TextureId.SELECTED_CELL), x, cellY, cellSize, cellSize);
            }
        }

        PauseButton.draw();

        // Cursor (siempre lo último, por encima de todo el HUD)
        Item selected = Player.selectedItem();
        if (Player.buildMode && selected != null && selected.itemKind.isBuildable()) {
            TextureId texture = Player.canBuild ? TextureId.BUILD_POINTER_OK : TextureId.BUILD_POINTER_BLOCKED;
            drawSprite(tex(texture), Mouse.getX() - 32, height - Mouse.getY() - 32, 64, 64);
        } else {
            drawSprite(tex(TextureId.POINTER), Mouse.getX() - 20 - hudScale / 2,
                    height - hudScale / 2 - Mouse.getY() - 20, 40 + hudScale, 40 + hudScale);
        }
    }

    private static void drawDroppedItems() {
        for (Chunk chunk : WorldManager.activeChunks) {
            for (DroppedItem dropped : chunk.droppedItems) {
                drawSprite(tex(dropped.item().icon), screenX(dropped.x()) - 16, screenY(dropped.y()) - 16, 32, 32);
            }
        }
    }

    private static Texture tex(TextureId id) {
        return TexturesManager.get(id);
    }
}
