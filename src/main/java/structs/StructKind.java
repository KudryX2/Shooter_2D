package structs;

import assets.TextureId;

/** Tipos de estructura que se pueden construir. */
public enum StructKind {
    /** Bloquea el paso y las balas. Un cuarto de casilla de grosor. */
    WALL("Wall", TextureId.WALL, TextureId.WALL, 16, true, true, true),
    /** Saco terrero: bloquea el paso pero deja pasar las balas. Tres dieciseisavos de casilla de grosor. */
    HALF_WALL("Half wall", TextureId.HALF_WALL, TextureId.HALF_WALL, 12, true, true, false),
    /** Ocupa una casilla entera y no bloquea nada. */
    FLOOR("Floor", TextureId.GRASS, TextureId.FLOOR_ICON, Struct.SIZE, false, false, false),
    /**
     * Caja de madera para guardar cosas (un {@code world.Container}): va en medio de una casilla, sin llenarla (12x8
     * px vista desde arriba, ver {@link #depth}), y bloquea el paso pero no las balas (es baja, como un saco terrero).
     */
    CRATE("Wooden crate", TextureId.CRATE, TextureId.CRATE_ICON, 48, false, true, false);

    public final String displayName;
    /** Textura con la que se construye (y la de los mapas). */
    public final TextureId texture;
    /**
     * Icono del material para construirla (inventario, suelo y mano). En los muros es la propia textura; el suelo
     * tiene uno aparte, porque su textura es una casilla entera y no cabría bien en una casilla del inventario.
     */
    public final TextureId icon;
    /** Grosor de un muro; en lo que va en una casilla, el ancho de lo que ocupa. */
    public final float thickness;
    /** Va sobre un borde entre casillas (un muro); si no, en medio de una casilla. */
    public final boolean isWall;
    /** Bloquea el paso a jugador y NPC. */
    public final boolean solid;
    public final boolean blocksBullets;
    private static final float CRATE_DEPTH = 32;

    StructKind(String displayName, TextureId texture, TextureId icon, float thickness, boolean isWall, boolean solid,
               boolean blocksBullets) {
        this.displayName = displayName;
        this.texture = texture;
        this.icon = icon;
        this.thickness = thickness;
        this.isWall = isWall;
        this.solid = solid;
        this.blocksBullets = blocksBullets;
    }

    /**
     * En lo que va en una casilla, el fondo (de norte a sur) de lo que ocupa: igual que el ancho salvo en la caja, que
     * es menos profunda para que en 3/4 se vea como un cubo y no como un armario.
     */
    public float depth() {
        return this == CRATE ? CRATE_DEPTH : thickness;
    }
}
