package items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Catálogo de objetos. Las armas son plantillas: usa {@link Gun#copy()} antes de dárselas a alguien. */
public final class ItemManager {
    public static final List<Gun> guns = new ArrayList<>();
    public static final List<StructItem> buildables = new ArrayList<>();

    public static final int MEDKIT_HEAL = 40;
    private static final int MEDKIT_TEXTURE = 0; // índice en TexturesManager.consumableTextures

    private ItemManager() {
    }

    public static void init() {
        guns.clear();
        // nombre, tipo, texturas (inventario, mano), alcance, velocidad, daño, cadencia (ms), dispersión (º)
        guns.add(new Gun("Pistol", GunKind.PISTOL, 0, 0, 600, 3, 35, 100, 2f));
        guns.add(new Gun("Shotgun", GunKind.SHOTGUN, 1, 1, 350, 3, 50, 400, 4f)); // por perdigón, sobre el abanico
        guns.add(new Gun("Sniper", GunKind.SNIPER, 2, 2, 1000, 5, 80, 500, 0.5f));
        guns.add(new Gun("AK-47", GunKind.ASSAULT_RIFLE, 3, 3, 800, 4, 25, 50, 6f));

        buildables.clear();
        buildables.add(new StructItem("Wall", ItemKind.WALL, 2));
        buildables.add(new StructItem("Half wall", ItemKind.HALF_WALL, 4));
        buildables.add(new StructItem("Floor", ItemKind.FLOOR, 3));
    }

    /** Botiquín nuevo (cada uno se gasta al usarlo). */
    public static Consumable medkit() {
        return new Consumable("Medkit", MEDKIT_TEXTURE, MEDKIT_HEAL);
    }

    public static Gun randomGun() {
        return guns.get(ThreadLocalRandom.current().nextInt(guns.size()));
    }
}
