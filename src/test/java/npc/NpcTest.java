package npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphics.DisplayManager;
import items.ItemManager;
import items.Projectile;
import player.Player;
import world.Chunk;
import world.WorldManager;

class NpcTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0)); // al morir sueltan su arma
        WorldManager.npcs.clear();
        WorldManager.projectiles.clear();
        ItemManager.init();
        NpcManager.init();
        Player.x = 0;
        Player.y = 0;
        Player.hp = 100;
        DisplayManager.delta = 8;
    }

    @Test
    void zombieNextToThePlayerBitesWithCooldown() {
        Npc zombie = NpcManager.spawnNpc(NpcManager.ZOMBIE, 60, 0, 0);

        zombie.update();
        assertTrue(Player.hp < 100, "el zombi debería morder");
        int afterFirstBite = Player.hp;

        zombie.update(); // dentro del tiempo de espera: no vuelve a morder
        assertEquals(afterFirstBite, Player.hp);
    }

    @Test
    void farZombieDoesNotBite() {
        Npc zombie = NpcManager.spawnNpc(NpcManager.ZOMBIE, 400, 0, 0);
        zombie.update();
        assertEquals(100, Player.hp);
    }

    @Test
    void convertedHumanRisesAfterTwoToFiveSeconds() throws Exception {
        Npc human = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 400, 0, 0);
        long start = System.currentTimeMillis();
        human.die(false);
        while (human.isRising() && System.currentTimeMillis() - start < 6_000) {
            human.update();
            Thread.sleep(20);
        }
        long elapsed = System.currentTimeMillis() - start;

        assertFalse(human.isRising(), "no se ha levantado");
        assertTrue(elapsed >= 2_000 && elapsed <= 5_200, "se levantó a los " + elapsed + " ms");
        assertEquals(Npc.TEXTURE_ZOMBIE_ALT, human.textureID);
    }

    @Test
    void hostileHumansIgnoreZombiesThatAreStillRising() {
        Player.x = 5_000; // lejos, para que el hostil no dispare al jugador
        Npc soldier = NpcManager.spawnNpc(NpcManager.HUMAN_AGGRESSIVE, 0, 0, 0);
        Npc victim = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 300, 0, 0);
        victim.die(false);

        soldier.update();
        assertTrue(WorldManager.projectiles.isEmpty(), "ha disparado a un zombi que aún no se ha levantado");
    }

    @Test
    void hostileHumansShootStandingZombies() {
        Player.x = 5_000;
        Npc soldier = NpcManager.spawnNpc(NpcManager.HUMAN_AGGRESSIVE, 0, 0, 0);
        NpcManager.spawnNpc(NpcManager.ZOMBIE, 300, 0, 0);

        soldier.update();
        assertFalse(WorldManager.projectiles.isEmpty());
    }

    @Test
    void bulletsCanFinishOffRisingZombies() {
        Player.x = 5_000;
        Npc victim = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 100, 0, 0);
        victim.die(false);
        WorldManager.projectiles.add(new Projectile(100, 0, 0, 0, 600, 999, 0));

        WorldManager.update(); // la bala impacta
        WorldManager.update(); // sin vida: muere del todo, ya no vuelve a levantarse
        assertFalse(victim.alive);
    }

    @Test
    void bulletHitIsRememberedForTheHitFlash() {
        Player.x = 5_000;
        Npc zombie = NpcManager.spawnNpc(NpcManager.ZOMBIE, 100, 0, 0);
        assertFalse(zombie.wasHitWithin(100));

        WorldManager.projectiles.add(new Projectile(100, 0, 0, 0, 600, 10, 0));
        WorldManager.update();
        assertTrue(zombie.wasHitWithin(100));
        assertEquals(90, zombie.hp);
    }

    @Test
    void zombieStillGettingUpDoesNotBite() {
        Npc human = NpcManager.spawnNpc(NpcManager.HUMAN_PASSIVE, 60, 0, 0);
        human.die(false); // se convierte en zombi, pero tarda en levantarse
        human.update();
        assertEquals(100, Player.hp);
    }
}
