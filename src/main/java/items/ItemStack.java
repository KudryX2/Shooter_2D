package items;

/** Un objeto y cuántos hay (1 si no se apila). P.ej. lo que hay en una casilla, o lo que suelta alguien al morir. */
public record ItemStack(Item item, int count) {
}
