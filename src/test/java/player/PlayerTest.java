package player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.Consumable;
import items.Gun;
import items.GunType;
import items.Projectile;
import structs.Struct;
import structs.StructKind;
import util.GameClock;
import world.Chunk;
import world.WorldManager;

class PlayerTest {
    private static final float HALF = Player.SIZE / 2;

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        GameClock.reset();
        Player.inventory.removeAll();
        Player.inventory.select(0);
        Player.x = 0;
        Player.y = 0;
        Player.hp = Player.MAX_HP;
    }

    /** Anda {@code frames} frames de 16 ms en la dirección (dx, dy). */
    private static void walk(int dx, int dy, int frames) {
        for (int i = 0; i < frames; i++) {
            GameClock.tick(16);
            Player.move(dx, dy);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Movimiento
    // ---------------------------------------------------------------------------------------------

    @Test
    void walksFreelyWithoutWalls() {
        walk(1, 0, 10);
        assertEquals(10 * 16 * 0.5f, Player.x, 0.01f);
        assertEquals(0, Player.y);
    }

    @Test
    void stopsRightAtAThinWall() {
        Struct wall = Struct.westWall(1, 0, StructKind.WALL); // vertical en x = 32.5, 16 px de grosor
        WorldManager.build(wall);

        walk(1, 0, 50);
        float touching = wall.x() - wall.halfWidth() - HALF;
        assertTrue(Player.x <= touching && Player.x > touching - 8, "se para pegado al muro: " + Player.x);
    }

    @Test
    void halfWallsAlsoBlockThePlayer() {
        WorldManager.build(Struct.westWall(1, 0, StructKind.HALF_WALL));
        walk(1, 0, 50);
        assertTrue(Player.x < 32.5f);
    }

    @Test
    void slidesAlongAWallWhenWalkingDiagonally() {
        WorldManager.build(Struct.westWall(1, 0, StructKind.WALL));
        WorldManager.build(Struct.westWall(1, 1, StructKind.WALL));
        walk(1, 1, 10);
        assertTrue(Player.x < 32.5f, "no atraviesa el muro");
        assertEquals(10 * 16 * 0.5f, Player.y, 0.01f, "pero sigue avanzando a lo largo de él");
    }

    @Test
    void aSlowFrameDoesNotGoThroughAThinWall() {
        WorldManager.build(Struct.westWall(1, 0, StructKind.WALL));
        GameClock.tick(500); // un frame de medio segundo: 250 px de golpe
        Player.move(1, 0);
        assertTrue(Player.x < 32.5f, "ha atravesado el muro: " + Player.x);
    }

    @Test
    void canWalkOutOfAWallBuiltOnTop() {
        WorldManager.build(Struct.westWall(0, 0, StructKind.WALL)); // en x = -32.5, tocando al jugador
        Player.x = -32.5f;
        walk(1, 0, 10);
        assertTrue(Player.x > -32.5f);
    }

    // ---------------------------------------------------------------------------------------------
    // Puntería y disparo
    // ---------------------------------------------------------------------------------------------

    @Test
    void looksAndShootsTowardsTheAimedPoint() {
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.aimAt(0, 300); // al sur
        assertEquals(90, Player.rotation, 0.01f);

        Player.primaryAction(true);
        Projectile bullet = WorldManager.projectiles.getFirst();
        assertEquals(0, bullet.x, 0.01f);
        assertEquals(Player.HAND_OFFSET, bullet.y, 0.01f, "sale de la mano, hacia el sur");
    }

    // ---------------------------------------------------------------------------------------------
    // Botiquines
    // ---------------------------------------------------------------------------------------------

    @Test
    void medkitHealsAndIsConsumed() {
        Player.hp = 30;
        Player.inventory.add(Consumable.medkit());

        assertTrue(Player.useSelectedConsumable());
        assertEquals(30 + Consumable.MEDKIT_HEAL, Player.hp);
        assertTrue(Player.inventory.isEmpty());
    }

    @Test
    void medkitNeverHealsAboveMaxHp() {
        Player.hp = 90;
        Player.inventory.add(Consumable.medkit());

        Player.useSelectedConsumable();
        assertEquals(Player.MAX_HP, Player.hp);
    }

    @Test
    void medkitIsNotWastedAtFullHealth() {
        Player.inventory.add(Consumable.medkit());

        assertFalse(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.size());
    }

    @Test
    void onlyTheSelectedSlotIsUsed() {
        Player.hp = 50;
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.inventory.add(Consumable.medkit());

        assertFalse(Player.useSelectedConsumable(), "la casilla 0 es una pistola");
        Player.inventory.select(1);
        assertTrue(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.size());
    }

    @Test
    void dyingDropsEverythingAndRespawnsWithFullHealth() {
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.inventory.add(Consumable.medkit());
        Player.hp = 0;

        Player.update();
        assertTrue(Player.inventory.isEmpty());
        assertEquals(2, WorldManager.chunks.getFirst().droppedItems.size());
        assertEquals(Player.MAX_HP, Player.hp);
        assertEquals(Player.SPAWN_X, Player.x);
    }
}
