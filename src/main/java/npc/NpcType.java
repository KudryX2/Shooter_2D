package npc;

import java.util.function.Supplier;

import assets.TextureId;

/** Tipos de NPC que se pueden crear con {@link NpcManager#spawn}. */
public enum NpcType {
    ZOMBIE(TextureId.ZOMBIE, NpcKind.ZOMBIE, ZombieBrain::new),
    /** Mismo zombi con otro aspecto, para que no todos sean iguales. */
    ZOMBIE_ALT(TextureId.ZOMBIE_ALT, NpcKind.ZOMBIE, ZombieBrain::new),
    /** Dispara a zombis y, si no hay, al jugador. */
    SOLDIER(TextureId.HUMAN, NpcKind.HUMAN, ShooterBrain::soldier),
    /** Aliado: solo dispara a zombis. */
    GUARD(TextureId.HUMAN, NpcKind.HUMAN, ShooterBrain::guard),
    /** Huye de los zombis. */
    CIVILIAN(TextureId.HUMAN, NpcKind.HUMAN, CivilianBrain::new);

    /** Velocidad al perseguir o huir, en px/ms. */
    public static final float SPEED = 0.25f;

    public final TextureId texture;
    public final NpcKind kind;
    /** Crea el cerebro de cada NPC nuevo de este tipo (cada uno el suyo). */
    final Supplier<NpcBrain> brain;

    NpcType(TextureId texture, NpcKind kind, Supplier<NpcBrain> brain) {
        this.texture = texture;
        this.kind = kind;
        this.brain = brain;
    }
}
