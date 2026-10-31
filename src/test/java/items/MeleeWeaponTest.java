package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import npc.Npc;
import npc.NpcType;
import npc.brain.ZombieBrain;
import player.Player;
import structs.Struct;
import structs.StructKind;
import util.GameClock;
import world.Chunk;
import world.Structures;
import world.WorldManager;

class MeleeWeaponTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        GameClock.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
    }

    private static Npc zombieAt(float x, float y) {
        Npc npc = new Npc(NpcType.ZOMBIE);
        npc.setPosition(x, y, 0);
        WorldManager.npcs.add(npc);
        return npc;
    }

    @Test
    void hitsEveryoneInFrontWithinReachAndArc() {
        MeleeWeapon knife = new MeleeWeapon(MeleeType.KNIFE);
        Npc front = zombieAt(80, 0);
        Npc slightlyAside = zombieAt(70, 30); // a unos 23º: dentro del arco de 70º
        Npc behind = zombieAt(-80, 0);
        Npc tooFar = zombieAt(MeleeType.KNIFE.reach + 10, 0);

        assertTrue(knife.swing(0, 0, 0));

        assertEquals(100 - MeleeType.KNIFE.damage, front.hp);
        assertEquals(100 - MeleeType.KNIFE.damage, slightlyAside.hp);
        assertEquals(100, behind.hp);
        assertEquals(100, tooFar.hp);
    }

    @Test
    void reachesZombiesThatStopToBite() {
        for (MeleeType type : MeleeType.values()) {
            assertTrue(type.reach > ZombieBrain.BITE_RANGE, type + " no llega a los zombis que se paran a morder");
        }
    }

    @Test
    void waitsBetweenSwings() {
        MeleeWeapon axe = new MeleeWeapon(MeleeType.AXE);
        assertTrue(axe.swing(0, 0, 0));
        assertFalse(axe.swing(0, 0, 0), "mismo instante");
        GameClock.tick(MeleeType.AXE.rate);
        assertTrue(axe.swing(0, 0, 0));
    }

    @Test
    void doesNotHitThroughWalls() {
        // Muro vertical en x = 32.5, entre el jugador y el zombi
        Structures.build(Struct.westWall(1, 0, StructKind.WALL));
        Npc zombie = zombieAt(80, 0);
        new MeleeWeapon(MeleeType.MACHETE).swing(0, 0, 0);
        assertEquals(100, zombie.hp);
    }

    @Test
    void batPushesTheTargetAway() {
        Npc zombie = zombieAt(80, 0);
        new MeleeWeapon(MeleeType.BAT).swing(0, 0, 0);
        assertEquals(80 + MeleeType.BAT.knockback, zombie.x, 0.01f);
        assertEquals(0, zombie.y, 0.01f);
    }

    /** Vida del jugador tras dejar que el zombi actúe {@code millis} ms, con el jugador en (0, 0). */
    private static int playerHpAfter(Npc zombie, long millis) {
        for (long t = 0; t < millis; t += 8) {
            GameClock.tick(8);
            zombie.update();
        }
        return Player.hp;
    }

    @Test
    void hitZombiesDoNotBiteWhileStunned() {
        Player.x = 0;
        Player.y = 0;
        Player.hp = 100;
        Npc unhit = zombieAt(50, 0);
        assertEquals(100, playerHpAfter(unhit, ZombieBrain.BITE_WINDUP_MS - 20), "aún preparando el mordisco");
        assertTrue(playerHpAfter(unhit, 50) < 100, "sin golpearlo, muerde");

        WorldManager.npcs.clear();
        Player.hp = 100;
        Npc hit = zombieAt(50, 0);
        new MeleeWeapon(MeleeType.KNIFE).swing(0, 0, 0);
        assertTrue(hit.isStunned());
        assertEquals(100, playerHpAfter(hit, MeleeType.KNIFE.stun - 10));
        assertEquals(100, playerHpAfter(hit, ZombieBrain.BITE_WINDUP_MS), "tras el aturdimiento vuelve a prepararlo");
        assertTrue(playerHpAfter(hit, 50) < 100, "y luego muerde");
    }

    @Test
    void aBlowCancelsTheBiteBeingPrepared() {
        Player.x = 0;
        Player.y = 0;
        Player.hp = 100;
        Npc zombie = zombieAt(50, 0);
        playerHpAfter(zombie, ZombieBrain.BITE_WINDUP_MS - 50); // casi a punto de morder
        new MeleeWeapon(MeleeType.KNIFE).swing(0, 0, 0);
        assertEquals(100, playerHpAfter(zombie, MeleeType.KNIFE.stun + ZombieBrain.BITE_WINDUP_MS - 20));
    }

    @Test
    void swingAnimationLastsAShortWhile() {
        MeleeWeapon knife = new MeleeWeapon(MeleeType.KNIFE);
        assertEquals(1, knife.swingProgress(), "sin golpear: en reposo");
        knife.swing(0, 0, 0);
        assertEquals(0, knife.swingProgress());
        GameClock.tick(MeleeWeapon.SWING_MS);
        assertEquals(1, knife.swingProgress());
    }

    @Test
    void angleBetweenWrapsAround() {
        assertEquals(20, MeleeWeapon.angleBetween(350, 10), 0.01f);
        assertEquals(-20, MeleeWeapon.angleBetween(10, 350), 0.01f);
    }
}
