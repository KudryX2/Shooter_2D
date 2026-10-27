package npc;

public enum NpcState {
    /** En pie: piensa, se mueve y ataca. */
    ACTIVE,
    /**
     * Humano recién convertido que todavía está en el suelo: no se mueve, no muerde y los NPC no le apuntan,
     * aunque las balas sí le dan.
     */
    RISING,
    /** Cadáver: desaparece al rato. */
    DEAD
}
