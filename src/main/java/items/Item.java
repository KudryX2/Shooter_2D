package items;

import assets.TextureId;

/**
 * Objeto de inventario. Qué es cada uno lo dice su clase: {@link Gun}, {@link MeleeWeapon}, {@link StructItem} o
 * {@link Consumable}.
 */
public abstract sealed class Item permits Gun, MeleeWeapon, StructItem, Consumable {
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
}
