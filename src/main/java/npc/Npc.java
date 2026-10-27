package npc;

import static util.Geometry.angleTo;
import static util.Geometry.dirX;
import static util.Geometry.dirY;

import java.util.concurrent.ThreadLocalRandom;

import assets.TextureId;
import items.Gun;
import items.Inventory;
import items.Item;
import util.GameClock;
import world.Movement;
import world.WorldManager;

/**
 * Un NPC: su cuerpo (posición, vida, inventario), su ciclo de vida ({@link NpcState}) y las acciones básicas que
 * usa su {@link NpcBrain} (mirar, andar, disparar). Qué hace en cada momento lo decide el cerebro.
 */
public class Npc {
    /** Tiempo (aleatorio entre estos dos valores) que tarda un humano convertido en levantarse como zombi. */
    private static final long RISE_MIN_MS = 2000;
    private static final long RISE_MAX_MS = 5000;
    private static final long CORPSE_TIME_MS = 4000;
    /** Distancia del centro a la boca del arma. */
    private static final float GUN_OFFSET = 50;
    private static final int INVENTORY_SLOTS = 4;
    /** Medio lado de la caja con la que choca contra los muros (su cuerpo visible mide ~22 px de radio). */
    public static final float BODY_HALF = 20;

    /** Tipo con el que apareció; si un humano se convierte, sigue siendo el mismo (ver {@link #kind}). */
    public final NpcType type;
    public NpcKind kind;
    public TextureId texture;

    public float x, y;
    /** Hacia dónde mira, en grados (ver {@link util.Geometry}). */
    public float rotation;
    public int hp = 100;
    public final Inventory inventory = new Inventory(INVENTORY_SLOTS);

    private NpcBrain brain;
    private NpcState state = NpcState.ACTIVE;
    private long deathTime;
    private long riseDelay;
    private long lastHitTime = GameClock.NEVER;

    public Npc(NpcType type) {
        this.type = type;
        this.kind = type.kind;
        this.texture = type.texture;
        this.brain = type.brain.get();
    }

    public void setPosition(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public void update() {
        switch (state) {
            case DEAD -> { }
            case RISING -> {
                if (hp <= 0) {
                    die(false);
                } else if (millisSinceDeath() > riseDelay) {
                    state = NpcState.ACTIVE;
                    texture = TextureId.ZOMBIE_ALT;
                }
            }
            case ACTIVE -> {
                if (hp <= 0) {
                    die(false);
                } else {
                    brain.update(this);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Acciones que usan los cerebros
    // ---------------------------------------------------------------------------------------------

    void lookAt(float targetX, float targetY) {
        if (targetX != x || targetY != y) {
            rotation = angleTo(x, y, targetX, targetY);
        }
    }

    /**
     * Avanza hacia donde mira a {@code speedFactor} de su velocidad; si hay un muro va girando hasta encontrar
     * una dirección libre.
     */
    void walk(float speedFactor) {
        float step = NpcType.SPEED * speedFactor * GameClock.delta();
        for (int attempt = 0; attempt < 360; attempt++) {
            float nextX = x + dirX(rotation) * step;
            float nextY = y + dirY(rotation) * step;
            if (Movement.canMove(x, y, nextX, nextY, Npc.BODY_HALF)) {
                x = nextX;
                y = nextY;
                return;
            }
            rotation++;
        }
    }

    /** Dispara hacia donde mira con la primera arma que lleve. */
    void shoot() {
        Gun gun = inventory.firstGun();
        if (gun != null) {
            float muzzleX = x + dirX(rotation) * GUN_OFFSET;
            float muzzleY = y + dirY(rotation) * GUN_OFFSET;
            gun.shoot(x, y, muzzleX, muzzleY, rotation, false, true);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Vida y muerte
    // ---------------------------------------------------------------------------------------------

    public void takeDamage(int damage) {
        hp -= damage;
        lastHitTime = GameClock.now();
    }

    /** Sin headshot un humano se convierte en zombi (tarda un rato en levantarse); si no, muere del todo. */
    public void die(boolean headshot) {
        deathTime = GameClock.now();

        if (kind == NpcKind.HUMAN && !headshot) {
            kind = NpcKind.ZOMBIE;
            brain = new ZombieBrain();
            hp = 100;
            state = NpcState.RISING;
            riseDelay = ThreadLocalRandom.current().nextLong(RISE_MIN_MS, RISE_MAX_MS + 1);
            return;
        }

        if (kind == NpcKind.ZOMBIE) {
            WorldManager.zombiesKilled++;
        }
        for (Item item : inventory.removeAll()) {
            WorldManager.dropItem(item, x, y);
        }
        hp = 0;
        texture = TextureId.BLOOD;
        state = NpcState.DEAD;
    }

    /** No es un cadáver (puede estar en pie o levantándose). */
    public boolean isAlive() {
        return state != NpcState.DEAD;
    }

    /** En pie: se mueve, ataca y cuenta como amenaza para los demás. */
    public boolean isActive() {
        return state == NpcState.ACTIVE;
    }

    /** Ver {@link NpcState#RISING}. */
    public boolean isRising() {
        return state == NpcState.RISING;
    }

    /** Cadáver que ya lleva suficiente tiempo en el suelo: se puede quitar del mundo. */
    public boolean canDisappear() {
        return state == NpcState.DEAD && millisSinceDeath() > CORPSE_TIME_MS;
    }

    /** {@code true} si ha recibido daño en los últimos {@code millis} ms (para el parpadeo al dibujarlo). */
    public boolean wasHitWithin(long millis) {
        return GameClock.since(lastHitTime) < millis;
    }

    private long millisSinceDeath() {
        return GameClock.since(deathTime);
    }
}
