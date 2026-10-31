package items;

import assets.TextureId;

/**
 * Objeto de inventario. Qué es cada uno lo dice su clase: {@link Gun}, {@link MeleeWeapon}, {@link StructItem},
 * {@link Consumable} o {@link AmmoBox}.
 */
public abstract sealed class Item permits Gun, MeleeWeapon, StructItem, Consumable, AmmoBox {
    public final String name;
    /** Textura en el inventario y en el suelo (las armas, en el suelo, se ven con la de la mano). */
    public final TextureId icon;
    /** Textura en la mano del jugador. */
    public final TextureId heldTexture;

    protected Item(String name, TextureId icon, TextureId heldTexture) {
        this.name = name;
        this.icon = icon;
        this.heldTexture = heldTexture;
    }

    /**
     * Cuántos caben en una casilla del inventario. 1 = no se apila: cada uno es único (p.ej. un arma lleva su propio
     * tiempo de recarga). Los que se apilan no deben cambiar nunca, porque una pila guarda uno solo y su cantidad.
     */
    public int maxStack() {
        return 1;
    }

    /** {@code other} puede ir en la misma pila que este (son el mismo objeto a efectos de juego). */
    public boolean stacksWith(Item other) {
        return false;
    }
}
