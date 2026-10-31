package structs;

import assets.TextureId;

/** Tipos de estructura que se pueden construir. */
public enum StructKind {
    /** Bloquea el paso y las balas. Un cuarto de casilla de grosor. */
    WALL("Wall", TextureId.WALL, TextureId.WALL, 16, true, true),
    /** Saco terrero: bloquea el paso pero deja pasar las balas. Tres dieciseisavos de casilla de grosor. */
    HALF_WALL("Half wall", TextureId.HALF_WALL, TextureId.HALF_WALL, 12, true, false),
    /** Ocupa una casilla entera y no bloquea nada. */
    FLOOR("Floor", TextureId.GRASS, TextureId.FLOOR_ICON, Struct.SIZE, false, false);

    public final String displayName;
    /** Textura con la que se construye (y la de los mapas). */
    public final TextureId texture;
    /**
     * Icono del material para construirla (inventario, suelo y mano). En los muros es la propia textura; el suelo
     * tiene uno aparte, porque su textura es una casilla entera y no cabría bien en una casilla del inventario.
     */
    public final TextureId icon;
    public final float thickness;
    /** Va sobre un borde entre casillas y bloquea el paso a jugador y NPC. */
    public final boolean isWall;
    public final boolean blocksBullets;

    StructKind(String displayName, TextureId texture, TextureId icon, float thickness, boolean isWall,
               boolean blocksBullets) {
        this.displayName = displayName;
        this.texture = texture;
        this.icon = icon;
        this.thickness = thickness;
        this.isWall = isWall;
        this.blocksBullets = blocksBullets;
    }
}
