package npc;

import assets.TextureId;

/** Tipos de NPC que se pueden crear con {@link NpcManager#spawn}. */
public enum NpcType {
    ZOMBIE(TextureId.ZOMBIE, NpcKind.ZOMBIE, NpcBehaviour.AGGRESSIVE),
    /** Mismo zombi con otro aspecto, para que no todos sean iguales. */
    ZOMBIE_ALT(TextureId.ZOMBIE_ALT, NpcKind.ZOMBIE, NpcBehaviour.AGGRESSIVE),
    /** Dispara a zombis y, si no hay, al jugador. */
    SOLDIER(TextureId.HUMAN, NpcKind.HUMAN, NpcBehaviour.AGGRESSIVE),
    /** Aliado: solo dispara a zombis. */
    GUARD(TextureId.HUMAN, NpcKind.HUMAN, NpcBehaviour.REACTIVE),
    /** Huye de los zombis. */
    CIVILIAN(TextureId.HUMAN, NpcKind.HUMAN, NpcBehaviour.PASSIVE);

    /** Velocidad al perseguir o huir, en px/ms. */
    public static final float SPEED = 0.25f;

    public final TextureId texture;
    public final NpcKind kind;
    public final NpcBehaviour behaviour;

    NpcType(TextureId texture, NpcKind kind, NpcBehaviour behaviour) {
        this.texture = texture;
        this.kind = kind;
        this.behaviour = behaviour;
    }
}
