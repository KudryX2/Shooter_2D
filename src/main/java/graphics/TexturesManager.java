package graphics;

import java.util.Arrays;
import java.util.List;

import org.newdawn.slick.opengl.Texture;

public final class TexturesManager {
    // Índices de guiTextures
    public static final int POINTER = 0;
    public static final int SELECTED_CELL = 1;
    public static final int NORMAL_CELL = 2;
    public static final int HP_HIGH = 3;
    public static final int BUILD_POINTER_OK = 4;
    public static final int BUILD_POINTER_BLOCKED = 5;
    public static final int HP_MEDIUM = 6;
    public static final int HP_LOW = 7;
    public static final int HP_BACKGROUND = 8;

    // Índices de environmentTextures
    public static final int ENV_TEEMO = 0;
    public static final int ENV_DIRT = 1;
    public static final int ENV_WALL = 2;
    public static final int ENV_GRASS = 3;
    public static final int ENV_HALF_WALL = 4;

    public static List<Texture> effectTextures = List.of();         // efectos
    public static List<Texture> playerTextures = List.of();         // jugador
    public static List<Texture> guiTextures = List.of();            // HUD
    public static List<Texture> environmentTextures = List.of();    // construcciones
    public static List<Texture> zombieTextures = List.of();         // NPCs
    public static List<Texture> weaponTextures = List.of();         // armas (inventario / suelo)
    public static List<Texture> weaponEquippedTextures = List.of(); // armas en la mano
    public static List<Texture> consumableTextures = List.of();     // botiquines

    private TexturesManager() {
    }

    /** Requiere un contexto OpenGL ya creado. */
    public static void init() {
        effectTextures = load("0-bullet");
        playerTextures = load("1-player");
        guiTextures = load("2-pointer", "2-selectedCell", "2-normalCell", "2-hp", "2-buildModePointer",
                "2-buildModePointer1", "2-hp1", "2-hp2", "2-hpFont");
        environmentTextures = load("3-Teemo", "3-dirt", "3-wall", "3-grass", "3-halfWall");
        // Orden según Npc.TEXTURE_*. No hay sprite propio para los humanos: se reutiliza el del jugador
        zombieTextures = load("4-zombie", "4-blood", "4-zombie1", "1-player");
        weaponTextures = load("5-pistol", "5-shootgun", "5-sniper", "5-ak47");
        consumableTextures = load("7-medkit");
        // No hay sprite "en la mano" para la AK-47: se reutiliza el del sniper
        weaponEquippedTextures = load("6-pistol", "6-shootgun", "6-sniper", "6-sniper");
    }

    private static List<Texture> load(String... names) {
        return Arrays.stream(names)
                .map(name -> Sprite.loadTexture("res/" + name + ".png"))
                .toList();
    }
}
