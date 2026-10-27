package items;

import static assets.TextureId.*;

import java.util.concurrent.ThreadLocalRandom;

import assets.TextureId;

/** Catálogo de armas con sus estadísticas. Cada {@link Gun} es una unidad concreta de uno de estos tipos. */
public enum GunType {
    //            nombre      inventario    mano          alcance vel. daño cadencia dispersión
    PISTOL("Pistol", PISTOL_ICON, PISTOL_HELD, 600, 3, 35, 100, 2f, false),
    /** Daño por perdigón; dispara un abanico de {@link Gun#SHOTGUN_PATTERN}. */
    SHOTGUN("Shotgun", SHOTGUN_ICON, SHOTGUN_HELD, 350, 3, 50, 400, 4f, false),
    SNIPER("Sniper", SNIPER_ICON, SNIPER_HELD, 1000, 5, 80, 500, 0.5f, false),
    ASSAULT_RIFLE("AK-47", AK47_ICON, AK47_HELD, 800, 4, 25, 50, 6f, true);

    public final String displayName;
    public final TextureId icon;
    public final TextureId heldTexture;
    /** Distancia máxima de las balas, en px. */
    public final int range;
    /** Velocidad de las balas, en px/ms. */
    public final float speed;
    public final int damage;
    /** Tiempo mínimo entre disparos, en milisegundos. */
    public final int rate;
    /** Desviación aleatoria máxima de cada bala, en grados a cada lado (0 = disparo perfecto). */
    public final float spread;
    /** Dispara mientras se mantiene el gatillo; las demás, una vez por pulsación. */
    public final boolean automatic;

    GunType(String displayName, TextureId icon, TextureId heldTexture, int range, float speed, int damage,
            int rate, float spread, boolean automatic) {
        this.displayName = displayName;
        this.icon = icon;
        this.heldTexture = heldTexture;
        this.range = range;
        this.speed = speed;
        this.damage = damage;
        this.rate = rate;
        this.spread = spread;
        this.automatic = automatic;
    }

    public static GunType random() {
        GunType[] all = values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }
}
