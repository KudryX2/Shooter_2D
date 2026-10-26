package assets;

/**
 * Todas las texturas del juego, cada una con su archivo en {@code res/}. El modelo (estructuras, objetos, NPC)
 * guarda un {@code TextureId}; solo {@code graphics.TexturesManager} sabe convertirlo en una textura de OpenGL.
 *
 * <p>Para añadir una textura basta con añadir una constante aquí: se carga sola al iniciar.
 * Varias constantes pueden compartir archivo (se carga una sola vez).
 */
public enum TextureId {
    // Efectos
    BULLET("0-bullet"),

    // Personajes
    PLAYER("1-player"),
    /** No hay sprite propio para los NPC humanos: se reutiliza el del jugador (tintado al dibujarlo). */
    HUMAN("1-player"),
    ZOMBIE("4-zombie"),
    ZOMBIE_ALT("4-zombie1"),
    /** Cadáveres y charcos de sangre. */
    BLOOD("4-blood"),

    // HUD
    POINTER("2-pointer"),
    BUILD_POINTER_OK("2-buildModePointer"),
    BUILD_POINTER_BLOCKED("2-buildModePointer1"),
    NORMAL_CELL("2-normalCell"),
    SELECTED_CELL("2-selectedCell"),
    HP_HIGH("2-hp"),
    HP_MEDIUM("2-hp1"),
    HP_LOW("2-hp2"),
    HP_BACKGROUND("2-hpFont"),

    // Entorno (suelos y muros)
    GRASS("3-grass"),
    /** Tierra original. Hay una alternativa en "3-dirt2". */
    DIRT("3-dirt"),
    WALL("3-wall"),
    HALF_WALL("3-halfWall"),
    /** Imagen, no textura repetible: no se gira al usarla de suelo. */
    TEEMO("3-Teemo"),

    // Armas: icono (inventario y suelo) y sprite en la mano
    PISTOL_ICON("5-pistol"),
    SHOTGUN_ICON("5-shootgun"),
    SNIPER_ICON("5-sniper"),
    AK47_ICON("5-ak47"),
    PISTOL_HELD("6-pistol"),
    SHOTGUN_HELD("6-shootgun"),
    SNIPER_HELD("6-sniper"),
    /** No hay sprite "en la mano" para la AK-47: se reutiliza el del sniper. */
    AK47_HELD("6-sniper"),

    // Consumibles
    MEDKIT("7-medkit");

    /** Nombre del archivo en {@code res/}, sin la extensión {@code .png}. */
    public final String file;

    TextureId(String file) {
        this.file = file;
    }
}
