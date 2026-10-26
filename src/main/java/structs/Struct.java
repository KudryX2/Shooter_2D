package structs;

import items.ItemKind;

/** Estructura construida en el mundo, centrada en (x, y). */
public record Struct(float x, float y, ItemKind kind, int textureID) {
    public static final float SIZE = 65;

    public Struct {
        if (!kind.isBuildable()) {
            throw new IllegalArgumentException("No es una estructura: " + kind);
        }
    }
}
