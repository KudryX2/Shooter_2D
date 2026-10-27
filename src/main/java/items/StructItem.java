package items;

import structs.StructKind;

/** Objeto de inventario que permite construir una estructura (muro, medio muro o suelo). No se gasta. */
public final class StructItem extends Item {
    public final StructKind kind;

    public StructItem(StructKind kind) {
        super(kind.displayName, kind.texture, kind.texture);
        this.kind = kind;
    }
}
