package items;

import assets.TextureId;

/** Objeto de inventario que permite construir una estructura (muro, medio muro o suelo). */
public final class StructItem extends Item {

    public StructItem(String name, ItemKind itemKind, TextureId texture) {
        super(name, itemKind, texture, texture);
        if (!itemKind.isBuildable()) {
            throw new IllegalArgumentException("No es una estructura: " + itemKind);
        }
    }
}
