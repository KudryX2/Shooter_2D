package items;

/** Objeto tirado en el suelo del mundo; {@code count} si es una pila (p.ej. un montón de muros). */
public record DroppedItem(Item item, int count, float x, float y) {
}
