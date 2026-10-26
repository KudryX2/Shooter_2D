package items;

public enum ItemKind {
    GUN, CONSUMABLE, WALL, HALF_WALL, FLOOR;

    public boolean isBuildable() {
        return this == WALL || this == HALF_WALL || this == FLOOR;
    }
}
