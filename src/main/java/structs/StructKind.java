package structs;

import assets.TextureId;

/** Tipos de estructura que se pueden construir. */
public enum StructKind {
    /** Bloquea el paso y las balas. Un cuarto de casilla de grosor. */
    WALL("Wall", TextureId.WALL, 16, true, true),
    /** Saco terrero: bloquea el paso pero deja pasar las balas. Un quinto de casilla de grosor. */
    HALF_WALL("Half wall", TextureId.HALF_WALL, 13, true, false),
    /** Ocupa una casilla entera y no bloquea nada. */
    FLOOR("Floor", TextureId.GRASS, Struct.SIZE, false, false);

    public final String displayName;
    /** Textura por defecto (la del objeto para construirlo y la de los mapas). */
    public final TextureId texture;
    public final float thickness;
    /** Va sobre un borde entre casillas y bloquea el paso a jugador y NPC. */
    public final boolean isWall;
    public final boolean blocksBullets;

    StructKind(String displayName, TextureId texture, float thickness, boolean isWall, boolean blocksBullets) {
        this.displayName = displayName;
        this.texture = texture;
        this.thickness = thickness;
        this.isWall = isWall;
        this.blocksBullets = blocksBullets;
    }
}
