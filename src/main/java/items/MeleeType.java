package items;

import static assets.TextureId.*;

import assets.TextureId;

/**
 * Catálogo de armas cuerpo a cuerpo con sus estadísticas. Cada {@link MeleeWeapon} es una unidad concreta.
 *
 * <p>El alcance va de los pies del atacante a los del golpeado. Tiene que ser mayor que la distancia a la que se paran
 * los zombis para morder ({@code ZombieBrain.ATTACK_DISTANCE}), o no se les podría dar.
 */
public enum MeleeType {
    //               nombre      inventario    mano          daño alcance arco cadencia empuje aturde
    KNIFE("Knife", KNIFE_ICON, KNIFE_HELD, 35, 95, 70, 300, 0, 350),
    MACHETE("Machete", MACHETE_ICON, MACHETE_HELD, 55, 105, 90, 450, 15, 500),
    AXE("Axe", AXE_ICON, AXE_HELD, 80, 105, 90, 800, 20, 700),
    BAT("Bat", BAT_ICON, BAT_HELD, 40, 110, 110, 550, 50, 700),
    SHOVEL("Shovel", SHOVEL_ICON, SHOVEL_HELD, 45, 115, 100, 650, 50, 650);

    public final String displayName;
    public final TextureId icon;
    public final TextureId heldTexture;
    public final int damage;
    /** Distancia máxima (de pies a pies) a la que da, en px. */
    public final float reach;
    /** Ancho del golpe, en grados (la mitad a cada lado de donde apunta). */
    public final float arc;
    /** Tiempo mínimo entre golpes, en milisegundos. */
    public final int rate;
    /** Cuánto aparta al golpeado, en px. */
    public final float knockback;
    /**
     * Cuánto deja aturdido al golpeado (sin moverse ni atacar), en milisegundos. Si es mayor que {@link #rate}, un
     * enemigo al que se le sigue pegando no llega a atacar; si es menor (el hacha), le queda un hueco.
     */
    public final long stun;

    MeleeType(String displayName, TextureId icon, TextureId heldTexture, int damage, float reach, float arc,
              int rate, float knockback, long stun) {
        this.displayName = displayName;
        this.icon = icon;
        this.heldTexture = heldTexture;
        this.damage = damage;
        this.reach = reach;
        this.arc = arc;
        this.rate = rate;
        this.knockback = knockback;
        this.stun = stun;
    }
}
