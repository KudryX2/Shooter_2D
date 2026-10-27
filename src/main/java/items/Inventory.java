package items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Objetos que lleva alguien (jugador o NPC), con un número máximo de casillas y una seleccionada. */
public final class Inventory implements Iterable<Item> {
    private final int capacity;
    private final List<Item> items = new ArrayList<>();
    private int selected;

    public Inventory(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public Item get(int slot) {
        return items.get(slot);
    }

    /** Lo guarda en la primera casilla libre. Devuelve {@code false} si no cabe. */
    public boolean add(Item item) {
        if (isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    /** Vacía el inventario y devuelve lo que llevaba (p.ej. para soltarlo al morir). */
    public List<Item> removeAll() {
        List<Item> all = new ArrayList<>(items);
        items.clear();
        return all;
    }

    // ---------------------------------------------------------------------------------------------
    // Casilla seleccionada (la "mano")
    // ---------------------------------------------------------------------------------------------

    public int selectedSlot() {
        return selected;
    }

    /** Selecciona una casilla (aunque esté vacía). */
    public void select(int slot) {
        if (slot >= 0 && slot < capacity) {
            selected = slot;
        }
    }

    /** Pasa a la casilla siguiente (+1) o anterior (-1), dando la vuelta. */
    public void cycle(int direction) {
        selected = Math.floorMod(selected + direction, capacity);
    }

    /** Objeto en la casilla seleccionada, o {@code null} si está vacía. */
    public Item selectedItem() {
        return selected < items.size() ? items.get(selected) : null;
    }

    /** Quita y devuelve el objeto de la casilla seleccionada, o {@code null} si está vacía. */
    public Item removeSelected() {
        return selected < items.size() ? items.remove(selected) : null;
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

    @Override
    public Iterator<Item> iterator() {
        return items.iterator();
    }
}
