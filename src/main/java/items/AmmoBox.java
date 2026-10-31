package items;

/**
 * Caja de balas de un tipo, con hasta {@link #CAPACITY}. No se apila: cada caja ocupa su casilla y lleva sus propias
 * balas, que es el número que se ve en ella. Al recargar, las armas sacan balas de las cajas de su tipo
 * ({@link Inventory#takeRounds}); una caja que se queda vacía desaparece.
 */
public final class AmmoBox extends Item {
    public static final int CAPACITY = 100;

    public final AmmoType type;
    private int rounds;

    public AmmoBox(AmmoType type, int rounds) {
        super(type.displayName, type.icon, type.icon);
        this.type = type;
        this.rounds = Math.clamp(rounds, 0, CAPACITY);
    }

    public int rounds() {
        return rounds;
    }

    public boolean isEmpty() {
        return rounds == 0;
    }

    /** Saca hasta {@code wanted} balas y devuelve cuántas ha sacado. */
    public int take(int wanted) {
        int taken = Math.min(wanted, rounds);
        rounds -= taken;
        return taken;
    }

    /**
     * Pasa a esta caja las balas de {@code other} (del mismo tipo) hasta llenarla; lo que no quepa se queda en
     * {@code other}.
     *
     * @return {@code true} si se han pasado balas
     */
    public boolean fillFrom(AmmoBox other) {
        if (other == this || other.type != type || rounds == CAPACITY || other.isEmpty()) {
            return false;
        }
        rounds += other.take(CAPACITY - rounds);
        return true;
    }
}
