package npc;

import static util.Geometry.distance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import graphics.DisplayManager;
import items.Gun;
import items.Item;
import player.Player;
import world.WorldManager;

public class Npc {
    private static final float CHASE_RANGE = 600;
    private static final float ATTACK_DISTANCE = 75;
    private static final float WANDER_RADIUS = 200;
    private static final float CHASE_SPEED = 0.25f;
    private static final float AGGRESSIVE_ZOMBIE_RANGE = 800;
    private static final float AGGRESSIVE_PLAYER_RANGE = 400;
    private static final float REACTIVE_RANGE = 800;
    private static final float FLEE_RANGE = 500;
    private static final long REVIVE_TIME_MS = 5000;
    private static final long CORPSE_TIME_MS = 4000;
    private static final int DEAD_TEXTURE = 1;
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

        // Un humano convertido en zombi tarda un rato en levantarse
        if (!canMove && millisSinceDeath() > REVIVE_TIME_MS) {
            canMove = true;
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

    private void updateHuman() {
        switch (behaviour) {
            case AGGRESSIVE -> {
                Npc zombie = findZombieInRange(AGGRESSIVE_ZOMBIE_RANGE, false);
                if (zombie != null) {
                    lookAt(zombie.x, zombie.y);
                    shoot();
                } else if (distance(x, y, Player.x, Player.y) < AGGRESSIVE_PLAYER_RANGE) {
                    lookAt(Player.x, Player.y);
                    shoot();
                }
            }
            case REACTIVE -> {
                Npc zombie = findZombieInRange(REACTIVE_RANGE, true);
                if (zombie != null) {
                    lookAt(zombie.x, zombie.y);
                    shoot();
                }
            }
            case PASSIVE -> {
                Npc zombie = findZombieInRange(FLEE_RANGE, true);
                if (zombie != null) {
                    lookAt(zombie.x, zombie.y);
                    rotation += 180;
                    walk();
                }
            }
        }
    }

    private Npc findZombieInRange(float range, boolean mustBeMoving) {
        for (Npc other : WorldManager.npcs) {
            if (other.kind == NpcKind.ZOMBIE && other.alive && (!mustBeMoving || other.canMove)
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
        float step = speed * DisplayManager.delta;
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
        deathTime = System.currentTimeMillis();

        if (kind == NpcKind.HUMAN && !headshot) {
            // Se convierte en zombi
            kind = NpcKind.ZOMBIE;
            hp = 100;
            canMove = false;
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
        textureID = DEAD_TEXTURE;
        alive = false;
        canMove = false;
    }

    private long millisSinceDeath() {
        return System.currentTimeMillis() - deathTime;
    }
}
