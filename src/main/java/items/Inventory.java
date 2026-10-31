package items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Objetos que lleva alguien (jugador o NPC), en casillas fijas: lo que se quita deja un hueco y lo demás no se mueve.
 * Cada casilla tiene un objeto y cuántos hay; los que se apilan ({@link Item#maxStack()}) comparten casilla.
 *
 * <p>Las primeras {@link #hotbarSize()} casillas son la barra rápida, de donde se elige lo que se lleva en la mano;
 * el resto es la mochila.
 */
public final class Inventory implements Iterable<Item> {
    private final Item[] items;
    private final int[] counts;
    private final int hotbarSize;
    private int selected;

    /** Solo barra rápida, sin mochila (p.ej. un NPC). */
    public Inventory(int capacity) {
        this(capacity, capacity);
    }

    public Inventory(int capacity, int hotbarSize) {
        this.items = new Item[capacity];
        this.counts = new int[capacity];
        this.hotbarSize = Math.min(hotbarSize, capacity);
    }

    public int capacity() {
        return items.length;
    }

    public int hotbarSize() {
        return hotbarSize;
    }

    /** Objeto de la casilla, o {@code null} si está vacía. */
    public Item get(int slot) {
        return items[slot];
    }

    /** Cuántos hay en la casilla (0 si está vacía). */
    public int count(int slot) {
        return counts[slot];
    }

    public boolean isEmpty() {
        for (Item item : items) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    /** {@code true} si cabe al menos uno más de {@code item} (en una pila suya o en un hueco). */
    public boolean canAdd(Item item) {
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] == null || roomInStack(slot, item) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Guarda uno. Devuelve {@code false} si no cabe. */
    public boolean add(Item item) {
        return add(item, 1) == 0;
    }

    /**
     * Guarda {@code count} de {@code item}: primero completa las pilas que ya haya de ese objeto y luego ocupa los
     * huecos, empezando por la barra rápida.
     *
     * @return cuántos no han cabido
     */
    public int add(Item item, int count) {
        Objects.requireNonNull(item);
        if (item.maxStack() == 1 && count > 1) {
            // El mismo objeto (p.ej. un arma, con su tiempo de recarga) acabaría en varias casillas
            throw new IllegalArgumentException(item.name + " no se apila");
        }
        int left = count;
        for (int slot = 0; slot < items.length && left > 0; slot++) {
            int moved = Math.min(left, roomInStack(slot, item));
            counts[slot] += moved;
            left -= moved;
        }
        for (int slot = 0; slot < items.length && left > 0; slot++) {
            if (items[slot] == null) {
                int moved = Math.min(left, item.maxStack());
                items[slot] = item;
                counts[slot] = moved;
                left -= moved;
            }
        }
        return left;
    }

    /** Cuántos más de {@code item} caben en la pila de la casilla (0 si está vacía o tiene otra cosa). */
    private int roomInStack(int slot, Item item) {
        Item there = items[slot];
        return there != null && there.stacksWith(item) ? there.maxStack() - counts[slot] : 0;
    }

    /** Quita uno de la casilla y lo devuelve, o {@code null} si está vacía. */
    public Item removeOne(int slot) {
        Item item = items[slot];
        if (item != null && --counts[slot] == 0) {
            items[slot] = null;
        }
        return item;
    }

    /** Vacía la casilla y devuelve lo que había, o {@code null} si estaba vacía. */
    public ItemStack removeStack(int slot) {
        Item item = items[slot];
        if (item == null) {
            return null;
        }
        ItemStack stack = new ItemStack(item, counts[slot]);
        items[slot] = null;
        counts[slot] = 0;
        return stack;
    }

    /**
     * Pasa la pila entera de la casilla {@code slot} a {@code target}, donde se guarda como al recoger algo
     * ({@link #add(Item, int)}: completando pilas y luego en huecos). Lo que no quepa se queda en la casilla.
     */
    public void transfer(int slot, Inventory target) {
        Item item = items[slot];
        if (item == null || target == this) {
            return;
        }
        int left = target.add(item, counts[slot]);
        counts[slot] = left;
        if (left == 0) {
            items[slot] = null;
        }
    }

    /**
     * Mueve lo de la casilla {@code from} a {@code to}: si allí hay una pila del mismo objeto la completa (y lo que
     * sobre se queda en {@code from}); si hay otra cosa, las intercambia.
     */
    public void move(int from, int to) {
        move(this, from, this, to);
    }

    /**
     * Como {@link #move(int, int)}, pero de la casilla {@code from} de {@code source} a la {@code to} de
     * {@code target} (p.ej. de un contenedor al inventario del jugador). Pueden ser el mismo inventario.
     */
    public static void move(Inventory source, int from, Inventory target, int to) {
        Item[] items = source.items;
        int[] counts = source.counts;
        Item[] toItems = target.items;
        int[] toCounts = target.counts;
        if ((source == target && from == to) || items[from] == null) {
            return;
        }
        // Caja de balas sobre otra del mismo tipo con sitio: le pasa las balas (y si se queda vacía, desaparece). Si
        // la de destino está llena, se intercambian como cualquier otro objeto.
        if (items[from] instanceof AmmoBox box && toItems[to] instanceof AmmoBox other && other.fillFrom(box)) {
            if (box.isEmpty()) {
                items[from] = null;
                counts[from] = 0;
            }
            return;
        }
        int room = target.roomInStack(to, items[from]);
        if (room > 0) {
            int moved = Math.min(room, counts[from]);
            toCounts[to] += moved;
            counts[from] -= moved;
            if (counts[from] == 0) {
                items[from] = null;
            }
            return;
        }
        Item item = toItems[to];
        int count = toCounts[to];
        toItems[to] = items[from];
        toCounts[to] = counts[from];
        items[from] = item;
        counts[from] = count;
    }

    // ---------------------------------------------------------------------------------------------
    // Munición (en cajas de balas)
    // ---------------------------------------------------------------------------------------------

    /** Balas de ese tipo que quedan, sumando todas sus cajas. */
    public int totalRounds(AmmoType type) {
        int total = 0;
        for (Item item : items) {
            if (item instanceof AmmoBox box && box.type == type) {
                total += box.rounds();
            }
        }
        return total;
    }

    /**
     * Saca hasta {@code wanted} balas de ese tipo de las cajas, empezando por las más vacías (así se acaban antes
     * las cajas a medias). Las cajas que se quedan vacías desaparecen.
     *
     * @return cuántas ha sacado
     */
    public int takeRounds(AmmoType type, int wanted) {
        int taken = 0;
        while (taken < wanted) {
            int slot = emptiestBox(type);
            if (slot < 0) {
                break;
            }
            AmmoBox box = (AmmoBox) items[slot];
            taken += box.take(wanted - taken);
            if (box.isEmpty()) {
                items[slot] = null;
                counts[slot] = 0;
            }
        }
        return taken;
    }

    /** Casilla de la caja de ese tipo con menos balas, o -1 si no hay ninguna. */
    private int emptiestBox(AmmoType type) {
        int best = -1;
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] instanceof AmmoBox box && box.type == type
                    && (best < 0 || box.rounds() < ((AmmoBox) items[best]).rounds())) {
                best = slot;
            }
        }
        return best;
    }

    /** Vacía el inventario y devuelve lo que llevaba (p.ej. para soltarlo al morir). */
    public List<ItemStack> removeAll() {
        List<ItemStack> all = new ArrayList<>();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack stack = removeStack(slot);
            if (stack != null) {
                all.add(stack);
            }
        }
        return all;
    }

    // ---------------------------------------------------------------------------------------------
    // Casilla seleccionada de la barra rápida (la "mano")
    // ---------------------------------------------------------------------------------------------

    public int selectedSlot() {
        return selected;
    }

    /** Selecciona una casilla de la barra rápida (aunque esté vacía). */
    public void select(int slot) {
        if (slot >= 0 && slot < hotbarSize) {
            selected = slot;
        }
    }

    /** Pasa a la casilla siguiente (+1) o anterior (-1) de la barra rápida, dando la vuelta. */
    public void cycle(int direction) {
        selected = Math.floorMod(selected + direction, hotbarSize);
    }

    /** Objeto en la casilla seleccionada, o {@code null} si está vacía. */
    public Item selectedItem() {
        return items[selected];
    }

    /** Quita uno de la casilla seleccionada y lo devuelve, o {@code null} si está vacía. */
    public Item removeSelected() {
        return removeOne(selected);
    }

    /** Primera arma que lleve, o {@code null}. */
    public Gun firstGun() {
        for (Item item : items) {
            if (item instanceof Gun gun) {
                return gun;
            }
        }
        return null;
    }

    /** Los objetos que lleva (uno por casilla ocupada, sin repetir los de una pila). */
    @Override
    public Iterator<Item> iterator() {
        List<Item> present = new ArrayList<>();
        for (Item item : items) {
            if (item != null) {
                present.add(item);
            }
        }
        return present.iterator();
    }
}
