package items;

import assets.TextureId;

/** Tipos de munición: cada arma usa uno ({@link GunType#ammo}) y solo recarga de cajas de ese tipo. */
public enum AmmoType {
    PISTOL("Pistol ammo", TextureId.AMMO_PISTOL),
    SHOTGUN("Shotgun shells", TextureId.AMMO_SHOTGUN),
    RIFLE("Rifle ammo", TextureId.AMMO_RIFLE),
    SNIPER("Sniper ammo", TextureId.AMMO_SNIPER);

    public final String displayName;
    /** Icono de su caja de balas. */
    public final TextureId icon;

    AmmoType(String displayName, TextureId icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}
