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
     * Mueve lo de la casilla {@code from} a {@code to}: si allí hay una pila del mismo objeto la completa (y lo que
     * sobre se queda en {@code from}); si hay otra cosa, las intercambia.
     */
    public void move(int from, int to) {
        if (from == to || items[from] == null) {
            return;
        }
        int room = roomInStack(to, items[from]);
        if (room > 0) {
            int moved = Math.min(room, counts[from]);
            counts[to] += moved;
            counts[from] -= moved;
            if (counts[from] == 0) {
                items[from] = null;
            }
            return;
        }
        Item item = items[to];
        int count = counts[to];
        items[to] = items[from];
        counts[to] = counts[from];
        items[from] = item;
        counts[from] = count;
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
