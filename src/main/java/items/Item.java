package items;

public abstract sealed class Item permits Gun, StructItem {
    public final String name;
    public final ItemKind itemKind;
    public final int cellTextureID;
    public final int equippedTextureID;

    protected Item(String name, ItemKind itemKind, int cellTextureID, int equippedTextureID) {
        this.name = name;
        this.itemKind = itemKind;
        this.cellTextureID = cellTextureID;
        this.equippedTextureID = equippedTextureID;
    }
}
