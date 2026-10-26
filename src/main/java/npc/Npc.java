package npc;

import static util.Geometry.distance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import items.Gun;
import items.Item;
import player.Player;
import util.GameClock;
import world.WorldManager;

public class Npc {
    private static final float CHASE_RANGE = 600;
    private static final float ATTACK_DISTANCE = 75;
    /** Algo más que ATTACK_DISTANCE para que el mordisco llegue aunque el jugador se aleje un poco. */
    private static final float BITE_RANGE = 90;
    private static final int BITE_DAMAGE = 10;
    private static final long BITE_COOLDOWN_MS = 800;
    private static final float WANDER_RADIUS = 200;
    private static final float CHASE_SPEED = 0.25f;
    private static final float AGGRESSIVE_ZOMBIE_RANGE = 800;
    private static final float AGGRESSIVE_PLAYER_RANGE = 400;
    private static final float REACTIVE_RANGE = 800;
    private static final float FLEE_RANGE = 500;
    /** Tiempo (aleatorio entre estos dos valores) que tarda un humano convertido en levantarse como zombi. */
    private static final long RISE_MIN_MS = 2000;
    private static final long RISE_MAX_MS = 5000;
    private static final long CORPSE_TIME_MS = 4000;
    // Índices de TexturesManager.zombieTextures
    public static final int TEXTURE_ZOMBIE = 0;
    public static final int TEXTURE_DEAD = 1;
    public static final int TEXTURE_ZOMBIE_ALT = 2;
    public static final int TEXTURE_HUMAN = 3;
    private static final float GUN_OFFSET = 50;

    public float x, y, rotation;
    private float startX, startY;
    public float speed;

    public NpcKind kind;
    public final NpcBehaviour behaviour;
    public int textureID;

    public int hp = 100;
    public boolean alive = true;
    public boolean canMove = true;
    public boolean canDisappear;
    private long deathTime;
    private boolean wandering;
    private long lastBite = GameClock.NEVER;
    private long riseDelay;
    private long lastHitTime = GameClock.NEVER;

    public final List<Item> inventory = new ArrayList<>();

    public Npc(float speed, int textureID, NpcKind kind, NpcBehaviour behaviour) {
        this.speed = speed;
        this.textureID = textureID;
        this.kind = kind;
        this.behaviour = behaviour;
    }

    /** Crea un NPC nuevo a partir de una plantilla. */
    public Npc(Npc template) {
        this(template.speed, template.textureID, template.kind, template.behaviour);
    }

    public void setPosition(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.rotation = rotation;
    }

    public void update() {
        if (!alive) {
            if (millisSinceDeath() > CORPSE_TIME_MS) {
                canDisappear = true;
            }
            return;
        }
        if (hp <= 0) {
            die(false);
            return;
        }

        switch (kind) {
            case ZOMBIE -> updateZombie();
            case HUMAN -> updateHuman();
        }

        // Un humano convertido en zombi tarda un rato en levantarse; hasta entonces conserva su aspecto
        if (isRising() && millisSinceDeath() > riseDelay) {
            canMove = true;
            textureID = TEXTURE_ZOMBIE_ALT;
        }
    }

    private void updateZombie() {
        if (!canMove) {
            return;
        }
        float playerDistance = distance(x, y, Player.x, Player.y);
        if (playerDistance < CHASE_RANGE) {
            wandering = false;
            speed = CHASE_SPEED;
            lookAt(Player.x, Player.y);
            if (playerDistance < BITE_RANGE) {
                bite();
            }
            if (playerDistance > ATTACK_DISTANCE) {
                walk();
            }
        } else {
            // Movimiento aleatorio alrededor de su punto de aparición
            if (!wandering) {
                wandering = true;
                speed /= 5;
                rotation = ThreadLocalRandom.current().nextInt(360);
            }
            if (distance(x, y, startX, startY) > WANDER_RADIUS) {
                lookAt(startX, startY);
            }
            walk();
        }
    }

    /** Ataque cuerpo a cuerpo al jugador, como mucho una vez cada {@link #BITE_COOLDOWN_MS}. */
    private void bite() {
        long now = GameClock.now();
        if (now - lastBite >= BITE_COOLDOWN_MS) {
            Player.hp -= BITE_DAMAGE;
            lastBite = now;
        }
    }

    private void updateHuman() {
        switch (behaviour) {
            case AGGRESSIVE -> {
                Npc zombie = findZombieInRange(AGGRESSIVE_ZOMBIE_RANGE);
                if (zombie != null) {
                    lookAt(zombie.x, zombie.y);
                    shoot();
                } else if (distance(x, y, Player.x, Player.y) < AGGRESSIVE_PLAYER_RANGE) {
                    lookAt(Player.x, Player.y);
                    shoot();
                }
            }
            case REACTIVE -> {
                Npc zombie = findZombieInRange(REACTIVE_RANGE);
                if (zombie != null) {
                    lookAt(zombie.x, zombie.y);
                    shoot();
                }
            }
            case PASSIVE -> {
                Npc zombie = findZombieInRange(FLEE_RANGE);
                if (zombie != null) {
                    lookAt(zombie.x, zombie.y);
                    rotation += 180;
                    walk();
                }
            }
        }
    }

    /** Zombi en pie más cercano en la lista; los que aún se están levantando no cuentan como amenaza. */
    private Npc findZombieInRange(float range) {
        for (Npc other : WorldManager.npcs) {
            if (other.kind == NpcKind.ZOMBIE && other.alive && other.canMove
                    && distance(x, y, other.x, other.y) < range) {
                return other;
            }
        }
        return null;
    }

    private void lookAt(float targetX, float targetY) {
        float dx = x - targetX;
        float dy = y - targetY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d == 0) {
            return;
        }
        float alpha = (float) Math.toDegrees(Math.acos(dy / d));
        rotation = targetX >= x ? alpha : -alpha;
    }

    private void shoot() {
        for (Item item : inventory) {
            if (item instanceof Gun gun) {
                double angle = Math.toRadians(-rotation);
                float shotX = x - (float) Math.sin(angle) * GUN_OFFSET;
                float shotY = y - (float) Math.cos(angle) * GUN_OFFSET;
                gun.shoot(shotX, shotY, rotation - 90, false, true);
                return;
            }
        }
    }

    /** Avanza; si hay un obstáculo va girando hasta encontrar una dirección libre. */
    private void walk() {
        float step = speed * GameClock.delta();
        // Si ya está dentro de un obstáculo le dejamos moverse para que pueda salir
        boolean stuck = WorldManager.isSolidAt(x, y);
        for (int attempt = 0; attempt < 360; attempt++) {
            double angle = Math.toRadians(-rotation + 180);
            float nextX = x + (float) (Math.sin(angle) * step);
            float nextY = y + (float) (Math.cos(angle) * step);
            if (stuck || !WorldManager.isSolidAt(nextX, nextY)) {
                x = nextX;
                y = nextY;
                return;
            }
            rotation++;
        }
    }

    public void die(boolean headshot) {
        deathTime = GameClock.now();

        if (kind == NpcKind.HUMAN && !headshot) {
            // Se convierte en zombi
            kind = NpcKind.ZOMBIE;
            hp = 100;
            canMove = false;
            riseDelay = ThreadLocalRandom.current().nextLong(RISE_MIN_MS, RISE_MAX_MS + 1);
            return;
        }

        if (kind == NpcKind.ZOMBIE) {
            WorldManager.zombiesKilled++;
        }
        for (Item item : inventory) {
            WorldManager.dropItem(item, x, y);
        }
        inventory.clear();
        hp = 0;
        textureID = TEXTURE_DEAD;
        alive = false;
        canMove = false;
    }

    public void takeDamage(int damage) {
        hp -= damage;
        lastHitTime = GameClock.now();
    }

    /** {@code true} si ha recibido daño en los últimos {@code millis} ms (para el parpadeo al dibujarlo). */
    public boolean wasHitWithin(long millis) {
        return GameClock.since(lastHitTime) < millis;
    }

    /**
     * Humano recién convertido que todavía está en el suelo: no se mueve, no muerde y los NPC no le apuntan,
     * aunque las balas sí le dan.
     */
    public boolean isRising() {
        return alive && kind == NpcKind.ZOMBIE && !canMove;
    }

    private long millisSinceDeath() {
        return GameClock.since(deathTime);
    }
}
