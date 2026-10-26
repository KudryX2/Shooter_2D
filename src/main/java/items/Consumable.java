package items;

/** Objeto de un solo uso (clic derecho). Por ahora solo botiquines. */
public final class Consumable extends Item {
    /** Vida que recupera al usarlo. */
    public final int heal;

    public Consumable(String name, int textureID, int heal) {
        super(name, ItemKind.CONSUMABLE, textureID, textureID);
        this.heal = heal;
    }
}
