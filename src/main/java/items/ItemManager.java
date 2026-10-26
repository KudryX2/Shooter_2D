package items;

import static assets.TextureId.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Catálogo de objetos. Las armas son plantillas: usa {@link Gun#copy()} antes de dárselas a alguien. */
public final class ItemManager {
    public static final List<Gun> guns = new ArrayList<>();
    public static final List<StructItem> buildables = new ArrayList<>();

    public static final int MEDKIT_HEAL = 40;

    private ItemManager() {
    }

    public static void init() {
        guns.clear();
        // nombre, tipo, texturas (inventario, mano), alcance, velocidad, daño, cadencia (ms), dispersión (º)
        guns.add(new Gun("Pistol", GunKind.PISTOL, PISTOL_ICON, PISTOL_HELD, 600, 3, 35, 100, 2f));
        guns.add(new Gun("Shotgun", GunKind.SHOTGUN, SHOTGUN_ICON, SHOTGUN_HELD, 350, 3, 50, 400, 4f)); // por perdigón
        guns.add(new Gun("Sniper", GunKind.SNIPER, SNIPER_ICON, SNIPER_HELD, 1000, 5, 80, 500, 0.5f));
        guns.add(new Gun("AK-47", GunKind.ASSAULT_RIFLE, AK47_ICON, AK47_HELD, 800, 4, 25, 50, 6f));

        buildables.clear();
        buildables.add(new StructItem("Wall", ItemKind.WALL, WALL));
        buildables.add(new StructItem("Half wall", ItemKind.HALF_WALL, HALF_WALL));
        buildables.add(new StructItem("Floor", ItemKind.FLOOR, GRASS));
    }

    /** Botiquín nuevo (cada uno se gasta al usarlo). */
    public static Consumable medkit() {
        return new Consumable("Medkit", MEDKIT, MEDKIT_HEAL);
    }

    public static Gun randomGun() {
        return guns.get(ThreadLocalRandom.current().nextInt(guns.size()));
    }
}
