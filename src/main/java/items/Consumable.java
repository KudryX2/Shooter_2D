package items;

import assets.TextureId;

/** Objeto de un solo uso (clic derecho). Por ahora solo botiquines. */
public final class Consumable extends Item {
    public static final int MEDKIT_HEAL = 40;

    /** Vida que recupera al usarlo. */
    public final int heal;

    public Consumable(String name, TextureId texture, int heal) {
        super(name, texture, texture);
        this.heal = heal;
    }

    /** Botiquín nuevo (cada uno se gasta al usarlo). */
    public static Consumable medkit() {
        return new Consumable("Medkit", TextureId.MEDKIT, MEDKIT_HEAL);
    }
}
