package items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Catálogo de objetos. Las armas son plantillas: usa {@link Gun#copy()} antes de dárselas a alguien. */
public final class ItemManager {
    public static final List<Gun> guns = new ArrayList<>();
    public static final List<StructItem> buildables = new ArrayList<>();

    private ItemManager() {
    }

    public static void init() {
        guns.clear();
        guns.add(new Gun("Pistol", GunKind.PISTOL, 0, 0, 600, 3, 35, 100));
        guns.add(new Gun("Shotgun", GunKind.SHOTGUN, 1, 1, 350, 3, 50, 400));
        guns.add(new Gun("Sniper", GunKind.SNIPER, 2, 2, 1000, 5, 80, 500));
        guns.add(new Gun("AK-47", GunKind.ASSAULT_RIFLE, 3, 3, 800, 4, 25, 50));

        buildables.clear();
        buildables.add(new StructItem("Wall", ItemKind.WALL, 2));
        buildables.add(new StructItem("Half wall", ItemKind.HALF_WALL, 4));
        buildables.add(new StructItem("Floor", ItemKind.FLOOR, 3));
    }

    public static Gun randomGun() {
        return guns.get(ThreadLocalRandom.current().nextInt(guns.size()));
    }
}
