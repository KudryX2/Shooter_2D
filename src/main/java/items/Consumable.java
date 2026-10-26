package items;

import assets.TextureId;

/** Objeto de un solo uso (clic derecho). Por ahora solo botiquines. */
public final class Consumable extends Item {
    /** Vida que recupera al usarlo. */
    public final int heal;

    public Consumable(String name, TextureId texture, int heal) {
        super(name, ItemKind.CONSUMABLE, texture, texture);
        this.heal = heal;
    }
}
