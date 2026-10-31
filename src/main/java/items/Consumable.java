package items;

import assets.TextureId;

/** Objeto de un solo uso (clic derecho). Por ahora solo botiquines. Se apila hasta {@link #MAX_STACK}. */
public final class Consumable extends Item {
    public static final int MEDKIT_HEAL = 40;
    public static final int MAX_STACK = 10;

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

    @Override
    public int maxStack() {
        return MAX_STACK;
    }

    @Override
    public boolean stacksWith(Item other) {
        return other instanceof Consumable consumable && consumable.name.equals(name) && consumable.heal == heal
                && consumable.icon == icon;
    }
}
