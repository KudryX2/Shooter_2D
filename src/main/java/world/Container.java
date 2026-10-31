package world;

import items.Inventory;
import structs.Struct;

/**
 * Algo del mundo donde guardar objetos (por ahora, cajas de madera): su estructura (dónde está y con qué choca) y
 * su inventario. Se construyen y se rompen con {@link Structures}; al romperse, lo que tenían queda esparcido
 * alrededor.
 */
public final class Container {
    /** Casillas de un contenedor: tantas como la mochila del jugador. */
    public static final int SLOTS = 30;

    public final Struct struct;
    public final Inventory inventory = new Inventory(SLOTS);
    private boolean removed;

    Container(Struct struct) {
        this.struct = struct;
    }

    /** Se ha roto: ya no está en el mundo (si alguien lo tenía abierto, debe cerrarlo). */
    public boolean isRemoved() {
        return removed;
    }

    void markRemoved() {
        removed = true;
    }
}
