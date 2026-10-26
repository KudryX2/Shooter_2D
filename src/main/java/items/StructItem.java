package items;

/** Objeto de inventario que permite construir una estructura (muro, medio muro o suelo). */
public final class StructItem extends Item {

    public StructItem(String name, ItemKind itemKind, int textureID) {
        super(name, itemKind, textureID, textureID);
        if (!itemKind.isBuildable()) {
            throw new IllegalArgumentException("No es una estructura: " + itemKind);
        }
    }
}
