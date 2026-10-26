package items;

import assets.TextureId;

public abstract sealed class Item permits Gun, StructItem, Consumable {
    public final String name;
    public final ItemKind itemKind;
    /** Textura en el inventario y en el suelo. */
    public final TextureId icon;
    /** Textura en la mano del jugador. */
    public final TextureId heldTexture;

    protected Item(String name, ItemKind itemKind, TextureId icon, TextureId heldTexture) {
        this.name = name;
        this.itemKind = itemKind;
        this.icon = icon;
        this.heldTexture = heldTexture;
    }
}
