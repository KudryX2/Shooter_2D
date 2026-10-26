package graphics;

import static graphics.Sprite.drawSprite;
import static graphics.TexturesManager.*;
import static util.Geometry.distance;

import java.util.List;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.opengl.Texture;

import items.DroppedItem;
import items.Gun;
import items.Item;
import items.Projectile;
import items.StructItem;
import npc.Npc;
import player.Player;
import structs.Struct;
import world.Chunk;
import world.WorldManager;

public final class GraphicsManager {
    private static final int CELL_SPACING = 50;

    /** Escala del HUD (teclas N/M). */
    public static float hudScale;

    private GraphicsManager() {
    }

    public static void init() {
        hudScale = DisplayManager.WIDTH / 100f;
    }

    public static void render() {
        drawEnvironment();
        drawNpcs();
        drawDroppedItems();
        drawPlayer();
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
        // Orden de dibujado: suelos -> proyectiles -> muros -> medios muros
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.floors);
        for (Projectile p : WorldManager.projectiles) {
            drawSprite(effectTextures.get(p.textureID), screenX(p.x) - 2.5f, screenY(p.y) - 2.5f, 5, 5, p.rotation);
        }
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.walls);
        for (Chunk chunk : WorldManager.activeChunks) drawStructs(chunk.halfWalls);
    }

    private static void drawStructs(List<Struct> structs) {
        float half = Struct.SIZE / 2;
        for (Struct s : structs) {
            if (inRenderDistance(s.x(), s.y())) {
                drawSprite(environmentTextures.get(s.textureID()),
                        screenX(s.x()) - half, screenY(s.y()) - half, Struct.SIZE, Struct.SIZE);
            }
        }
    }

    private static void drawNpcs() {
        for (Npc npc : WorldManager.npcs) {
            if (!inRenderDistance(npc.x, npc.y)) {
                continue;
            }
            drawSprite(zombieTextures.get(npc.textureID), screenX(npc.x) - 32, screenY(npc.y) - 32, 64, 64, npc.rotation);
            if (npc.hp > 0 && npc.hp < 100) {
                drawHealthBar(npc.hp, screenX(npc.x) - 32, screenY(npc.y) - 45);
            }
        }
    }

    private static void drawHealthBar(int hp, float x, float y) {
        drawSprite(guiTextures.get(HP_BACKGROUND), x, y, 50, 5);
        int texture = hp > 50 ? HP_HIGH : hp > 25 ? HP_MEDIUM : HP_LOW;
        drawSprite(guiTextures.get(texture), x, y, hp / 2f, 5);
    }

    private static void drawPlayer() {
        float centerX = DisplayManager.WIDTH / 2f;
        float centerY = DisplayManager.HEIGHT / 2f;

        // El sprite del jugador está girado 90º
        drawSprite(playerTextures.get(Player.textureID), centerX - 50, centerY - 50, 100, 100, Player.rotation - 90);
        if (Player.hp > 0) {
            drawHealthBar(Player.hp, centerX - Player.SIZE / 2, centerY - Player.SIZE - 5);
        }

        // Objeto en la mano
        double angle = Math.toRadians(-Player.rotation + 90);
        float handX = centerX + (float) Math.sin(angle) * 40;
        float handY = centerY + (float) Math.cos(angle) * 40;

        switch (Player.selectedItem()) {
            case null -> { }
            case Gun gun -> drawSprite(weaponEquippedTextures.get(gun.equippedTextureID),
                    handX - 25, handY - 25, 50, 50, Player.rotation);
            case StructItem struct -> {
                if (Player.buildMode) {
                    drawSprite(environmentTextures.get(struct.equippedTextureID),
                            handX - 20, handY - 20, 40, 40, Player.rotation);
                }
            }
        }
    }

    private static void drawHud() {
        float width = DisplayManager.WIDTH;
        float height = DisplayManager.HEIGHT;
        float cellY = height / 1.1f;
        float cellSize = 50 + hudScale;

        // Inventario
        for (int i = 0; i < Player.inventory.size(); i++) {
            float x = width / 2 + 5 + (CELL_SPACING + hudScale) * i;
            drawSprite(cellTexture(Player.inventory.get(i)), x, cellY + 5, 40 + hudScale, 40 + hudScale);
        }

        // Casillas de la mano
        for (int i = 0; i < Player.HAND_SLOTS; i++) {
            float x = width / 2 + (CELL_SPACING + hudScale) * i;
            drawSprite(guiTextures.get(NORMAL_CELL), x, cellY, cellSize, cellSize);
            if (i == Player.selectedCell) {
                drawSprite(guiTextures.get(SELECTED_CELL), x, cellY, cellSize, cellSize);
            }
        }

        // Cursor
        Item selected = Player.selectedItem();
        if (Player.buildMode && selected != null && selected.itemKind.isBuildable()) {
            int texture = Player.canBuild ? BUILD_POINTER_OK : BUILD_POINTER_BLOCKED;
            drawSprite(guiTextures.get(texture), Mouse.getX() - 32, height - Mouse.getY() - 32, 64, 64);
        } else {
            drawSprite(guiTextures.get(POINTER), Mouse.getX() - 20 - hudScale / 2,
                    height - hudScale / 2 - Mouse.getY() - 20, 40 + hudScale, 40 + hudScale);
        }
    }

    private static void drawDroppedItems() {
        for (Chunk chunk : WorldManager.activeChunks) {
            for (DroppedItem dropped : chunk.droppedItems) {
                drawSprite(cellTexture(dropped.item()), screenX(dropped.x()) - 16, screenY(dropped.y()) - 16, 32, 32);
            }
        }
    }

    /** Textura del objeto en el inventario / en el suelo. */
    private static Texture cellTexture(Item item) {
        return switch (item) {
            case Gun gun -> weaponTextures.get(gun.cellTextureID);
            case StructItem struct -> environmentTextures.get(struct.cellTextureID);
        };
    }
}
