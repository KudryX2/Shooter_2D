package assets;

/**
 * Todas las texturas del juego, cada una con su imagen en {@code assets-raw/textures/}. Al compilar se juntan
 * todas en un atlas ({@link Assets#ATLAS}). El modelo (estructuras, objetos, NPC) guarda un {@code TextureId}; solo
 * {@code graphics.TexturesManager} sabe convertirlo en una textura de OpenGL.
 *
 * <p>Para añadir una textura basta con dejar su imagen en esa carpeta y añadir una constante aquí. Varias
 * constantes pueden compartir imagen. Para animarla, en vez de {@code nombre.png} se ponen sus frames como
 * {@code nombre_0.png}, {@code nombre_1.png}...
 */
public enum TextureId {
    // Efectos
    BULLET("0-bullet"),

    // Personajes en pixel art, vistos en 3/4 (ver {@link #character}).
    PLAYER("1-player", true),
    /** Todos los NPC humanos: se tintan al dibujarlos según su tipo. */
    HUMAN("4-human", true),
    ZOMBIE("4-zombie", true),
    ZOMBIE_ALT("4-zombieAlt", true),
    /** Cadáveres y charcos de sangre. */
    BLOOD("4-blood"),

    // HUD
    POINTER("2-pointer"),
    NORMAL_CELL("2-normalCell"),
    SELECTED_CELL("2-selectedCell"),
    HP_HIGH("2-hp"),
    HP_MEDIUM("2-hp1"),
    HP_LOW("2-hp2"),
    HP_BACKGROUND("2-hpFont"),

    // Entorno, en pixel art (assets-raw/textures/pixel, generados con la tarea generateArt). En los suelos, las
    // imágenes numeradas no son una animación sino variantes: cada casilla elige una según su posición.
    GRASS("3-grass"),
    DIRT("3-dirt"),
    /** Icono del muro (inventario, suelo y mano). En el mundo se dibuja con {@link #WALL_H} / {@link #WALL_V}. */
    WALL("3-wall"),
    /** Muro visto en 3/4 de este a oeste: remate arriba y cara frontal debajo. */
    WALL_H("3-wall-h"),
    /** Muro visto en 3/4 de norte a sur: remate largo y estrecho, con la cara frontal en su extremo sur. */
    WALL_V("3-wall-v"),
    /** Icono del saco terrero. En el mundo se dibuja con {@link #HALF_WALL_H} / {@link #HALF_WALL_V}. */
    HALF_WALL("3-halfWall"),
    HALF_WALL_H("3-halfWall-h"),
    HALF_WALL_V("3-halfWall-v"),

    // Armas en pixel art: icono (inventario y suelo) y en la mano (con la empuñadura en el centro de la imagen)
    PISTOL_ICON("5-pistol"),
    SHOTGUN_ICON("5-shootgun"),
    SNIPER_ICON("5-sniper"),
    AK47_ICON("5-ak47"),
    PISTOL_HELD("6-pistol"),
    SHOTGUN_HELD("6-shootgun"),
    SNIPER_HELD("6-sniper"),
    AK47_HELD("6-ak47"),

    // Consumibles
    MEDKIT("7-medkit");

    /** Direcciones de los personajes, tal y como aparecen en el nombre de sus imágenes. */
    public static final String[] DIRECTIONS = {"down", "up", "left", "right"};

    /**
     * Nombre de la imagen en {@code assets-raw/textures/} (y de su región en el atlas), sin {@code .png}. En los
     * personajes es solo el principio del nombre (ver {@link #character}).
     */
    public final String file;
    /**
     * Personaje con varias imágenes por dirección ({@link #DIRECTIONS}): {@code file-idle-dir} (quieto),
     * {@code file-walk-dir_0..n} (andando) y, si puede quedarse tumbado, {@code file-lying}.
     */
    public final boolean character;

    TextureId(String file) {
        this(file, false);
    }

    TextureId(String file, boolean character) {
        this.file = file;
        this.character = character;
    }

    /** Nombre en el atlas de una pose de personaje, p.ej. {@code pose("walk", "left")}. */
    public String pose(String pose, String direction) {
        return file + "-" + pose + "-" + direction;
    }

    /** Nombre en el atlas del personaje tumbado (solo lo tienen los que se pueden quedar en el suelo). */
    public String lying() {
        return file + "-lying";
    }
}
