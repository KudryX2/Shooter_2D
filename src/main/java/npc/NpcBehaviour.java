package npc;

public enum NpcBehaviour {
    /** Dispara a zombis y, si no hay, al jugador. */
    AGGRESSIVE,
    /** Solo dispara a zombis. */
    REACTIVE,
    /** Huye de los zombis. */
    PASSIVE
}
