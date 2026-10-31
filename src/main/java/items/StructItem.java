package items;

import structs.StructKind;

/**
 * Material para construir una estructura (muro, medio muro o suelo). Se gasta uno por cada estructura construida y
 * se recupera al derribarla. Se apila hasta {@link #MAX_STACK}.
 */
public final class StructItem extends Item {
    public static final int MAX_STACK = 20;

    public final StructKind kind;

    public StructItem(StructKind kind) {
        super(kind.displayName, kind.icon, kind.icon);
        this.kind = kind;
    }

    @Override
    public int maxStack() {
        return MAX_STACK;
    }

    @Override
    public boolean stacksWith(Item other) {
        return other instanceof StructItem struct && struct.kind == kind;
    }
}
