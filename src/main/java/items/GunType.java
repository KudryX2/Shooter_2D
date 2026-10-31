package items;

import static assets.TextureId.*;

import java.util.concurrent.ThreadLocalRandom;

import assets.TextureId;

/** Catálogo de armas con sus estadísticas. Cada {@link Gun} es una unidad concreta de uno de estos tipos. */
public enum GunType {
    // nombre, inventario, mano, alcance, vel., daño, cadencia, dispersión, auto, cañón, empuje,
    // munición, cargador, recarga (ms)
    PISTOL("Pistol", PISTOL_ICON, PISTOL_HELD, 600, 3, 35, 100, 2f, false, 2, 0,
            AmmoType.PISTOL, 12, 1000),
    /** Daño por perdigón; dispara un abanico de {@link Gun#SHOTGUN_PATTERN}. */
    SHOTGUN("Shotgun", SHOTGUN_ICON, SHOTGUN_HELD, 350, 3, 25, 400, 4f, false, 2, 20,
            AmmoType.SHOTGUN, 6, 2200),
    SNIPER("Sniper", SNIPER_ICON, SNIPER_HELD, 1000, 5, 80, 500, 0.5f, false, 1, 30,
            AmmoType.SNIPER, 5, 2600),
    ASSAULT_RIFLE("AK-47", AK47_ICON, AK47_HELD, 800, 4, 25, 50, 6f, true, 2, 0,
            AmmoType.RIFLE, 30, 1300);

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
    /**
     * Píxeles del dibujo que el cañón queda por encima de la empuñadura en su sprite de la mano (apuntando a la
     * derecha), para que las balas salgan por la boca. Tiene que cuadrar con {@code tools.GunArt}.
     */
    public final int barrelRise;
    /**
     * Cuánto aparta cada bala al NPC al que le da, en px (al jugador no lo mueven). En la escopeta es por perdigón:
     * a bocajarro le dan todos y empuja mucho más.
     */
    public final float knockback;
    /** Munición que usa: solo recarga de cajas de este tipo. */
    public final AmmoType ammo;
    /** Balas que caben en el cargador. Un disparo gasta una (en la escopeta, todos sus perdigones). */
    public final int magazine;
    /** Lo que tarda en recargar, en milisegundos. */
    public final long reloadMs;

    GunType(String displayName, TextureId icon, TextureId heldTexture, int range, float speed, int damage,
            int rate, float spread, boolean automatic, int barrelRise, float knockback, AmmoType ammo, int magazine,
            long reloadMs) {
        this.displayName = displayName;
        this.icon = icon;
        this.heldTexture = heldTexture;
        this.range = range;
        this.speed = speed;
        this.damage = damage;
        this.rate = rate;
        this.spread = spread;
        this.automatic = automatic;
        this.barrelRise = barrelRise;
        this.knockback = knockback;
        this.ammo = ammo;
        this.magazine = magazine;
        this.reloadMs = reloadMs;
    }

    public static GunType random() {
        GunType[] all = values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }
}
