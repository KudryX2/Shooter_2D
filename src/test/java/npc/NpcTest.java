package npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.Projectile;
import player.Player;
import util.GameClock;
import world.Chunk;
import world.WorldManager;

class NpcTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0)); // al morir sueltan su arma
        WorldManager.npcs.clear();
        WorldManager.projectiles.clear();
        Player.x = 0;
        Player.y = 0;
        Player.hp = 100;
        Player.aimX = Float.NaN; // no apunta a nada: sin headshots salvo que el test lo pida
        Player.aimY = Float.NaN;
        GameClock.reset();
        GameClock.tick(8);
    }

    @Test
    void zombieNextToThePlayerBitesWithCooldown() {
        Npc zombie = NpcManager.spawn(NpcType.ZOMBIE, 60, 0, 0);

        zombie.update();
        assertTrue(Player.hp < 100, "el zombi debería morder");
        int afterFirstBite = Player.hp;

        zombie.update(); // dentro del tiempo de espera: no vuelve a morder
        assertEquals(afterFirstBite, Player.hp);
    }

    @Test
    void farZombieDoesNotBite() {
        Npc zombie = NpcManager.spawn(NpcType.ZOMBIE, 400, 0, 0);
        zombie.update();
        assertEquals(100, Player.hp);
    }

    @Test
    void convertedHumanRisesAfterTwoToFiveSeconds() {
        Npc human = NpcManager.spawn(NpcType.CIVILIAN, 400, 0, 0);
        long start = GameClock.now();
        human.die(false);
        while (human.isRising() && GameClock.since(start) < 6_000) {
            GameClock.tick(20);
            human.update();
        }
        long elapsed = GameClock.since(start);

        assertFalse(human.isRising(), "no se ha levantado");
        assertTrue(elapsed >= 2_000 && elapsed <= 5_200, "se levantó a los " + elapsed + " ms");
        assertEquals(TextureId.ZOMBIE_ALT, human.texture);
    }

    @Test
    void hostileHumansIgnoreZombiesThatAreStillRising() {
        Player.x = 5_000; // lejos, para que el hostil no dispare al jugador
        Npc soldier = NpcManager.spawn(NpcType.SOLDIER, 0, 0, 0);
        Npc victim = NpcManager.spawn(NpcType.CIVILIAN, 300, 0, 0);
        victim.die(false);

        soldier.update();
        assertTrue(WorldManager.projectiles.isEmpty(), "ha disparado a un zombi que aún no se ha levantado");
    }

    @Test
    void hostileHumansShootStandingZombies() {
        Player.x = 5_000;
        Npc soldier = NpcManager.spawn(NpcType.SOLDIER, 0, 0, 0);
        NpcManager.spawn(NpcType.ZOMBIE, 300, 0, 0);

        soldier.update();
        assertFalse(WorldManager.projectiles.isEmpty());
    }

    @Test
    void bulletsCanFinishOffRisingZombies() {
        Player.x = 5_000;
        Npc victim = NpcManager.spawn(NpcType.CIVILIAN, 100, 0, 0);
        victim.die(false);
        WorldManager.projectiles.add(new Projectile(100, 0, 0, 0, 600, 999, false));

        WorldManager.update(); // la bala impacta
        WorldManager.update(); // sin vida: muere del todo, ya no vuelve a levantarse
        assertFalse(victim.isAlive());
    }

    @Test
    void onlyPlayerBulletsCanHeadshot() {
        Player.x = 5_000;
        Npc target = NpcManager.spawn(NpcType.CIVILIAN, 100, 0, 0);
        Player.aimX = 100; // el jugador apunta a la cabeza del objetivo (se dibuja ~54 px sobre sus pies)
        Player.aimY = -54;

        // Bala de un NPC: aunque el jugador apunte ahí, es un impacto normal (se convierte en zombi)
        WorldManager.projectiles.add(new Projectile(100, 0, 0, 0, 600, 999, false));
        WorldManager.update(); // impacto
        WorldManager.update(); // sin vida: se convierte
        assertTrue(target.isAlive());
        assertTrue(target.isRising());

        // Bala del jugador apuntando a su cabeza: headshot, muere del todo
        Npc second = NpcManager.spawn(NpcType.CIVILIAN, 300, 0, 0);
        Player.aimX = 300;
        WorldManager.projectiles.add(new Projectile(300, 0, 0, 0, 600, 1, true));
        WorldManager.update();
        assertFalse(second.isAlive());
    }

    @Test
    void playerBulletWithoutAimingAtTargetIsANormalHit() {
        Player.x = 5_000;
        Npc target = NpcManager.spawn(NpcType.CIVILIAN, 100, 0, 0);
        Player.aimX = 900; // apunta a otro sitio
        Player.aimY = 0;

        WorldManager.projectiles.add(new Projectile(100, 0, 0, 0, 600, 10, true));
        WorldManager.update();
        assertTrue(target.isAlive());
        assertEquals(90, target.hp);
    }

    @Test
    void pauseFreezesNpcsAndTimers() {
        Player.x = 0;
        Npc zombie = NpcManager.spawn(NpcType.ZOMBIE, 300, 0, 0);
        Npc human = NpcManager.spawn(NpcType.CIVILIAN, 2_000, 0, 0);
        human.die(false);

        GameClock.togglePause();
        long before = GameClock.now();
        for (int i = 0; i < 1_000; i++) {
            GameClock.tick(20); // 20 s de tiempo real
            WorldManager.update();
        }
        assertEquals(before, GameClock.now());
        assertEquals(0, GameClock.delta());
        assertEquals(300, zombie.x, "un zombi no debería moverse en pausa");
        assertTrue(human.isRising(), "no debería levantarse en pausa");

        GameClock.togglePause();
        GameClock.tick(8);
        WorldManager.update();
        assertTrue(zombie.x < 300, "al quitar la pausa vuelve a perseguir al jugador");
    }

    @Test
    void bulletHitIsRememberedForTheHitFlash() {
        Player.x = 5_000;
        Npc zombie = NpcManager.spawn(NpcType.ZOMBIE, 100, 0, 0);
        assertFalse(zombie.wasHitWithin(100));

        WorldManager.projectiles.add(new Projectile(100, 0, 0, 0, 600, 10, false));
        WorldManager.update();
        assertTrue(zombie.wasHitWithin(100));
        assertEquals(90, zombie.hp);
    }

    @Test
    void civiliansRunAwayFromZombies() {
        Player.x = 5_000;
        Npc civilian = NpcManager.spawn(NpcType.CIVILIAN, 0, 0, 0);
        NpcManager.spawn(NpcType.ZOMBIE, 200, 0, 0);
        civilian.update();
        assertTrue(civilian.x < 0, "debería alejarse del zombi (hacia -x), está en " + civilian.x);
    }

    @Test
    void soldiersShootThePlayerButGuardsDoNot() {
        Player.x = 300;
        Npc guard = NpcManager.spawn(NpcType.GUARD, 0, 0, 0);
        guard.update();
        assertTrue(WorldManager.projectiles.isEmpty(), "un guardia no dispara al jugador");

        Npc soldier = NpcManager.spawn(NpcType.SOLDIER, 0, 0, 0);
        soldier.update();
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void npcBulletsFlyTowardsTheTarget() {
        Player.x = 5_000;
        Npc soldier = NpcManager.spawn(NpcType.SOLDIER, 0, 0, 0);
        NpcManager.spawn(NpcType.ZOMBIE, 0, 300, 0); // al sur
        soldier.update();
        var bullet = WorldManager.projectiles.getFirst();
        float muzzle = Npc.GUN_OFFSET - 1; // sale de la mano, a GUN_OFFSET del centro
        assertTrue(bullet.y > muzzle && Math.abs(bullet.x) < 15, "sale hacia el sur: " + bullet.x + "," + bullet.y);
        bullet.move();
        assertTrue(bullet.y > muzzle + bullet.speed * GameClock.delta() - 1, "y sigue hacia el sur");
    }

    @Test
    void convertedHumansChaseThePlayerOnceTheyRise() {
        Npc human = NpcManager.spawn(NpcType.CIVILIAN, 300, 0, 0);
        human.die(false);
        GameClock.tick(5_100);
        human.update(); // se levanta
        assertTrue(human.isActive());
        GameClock.tick(8);
        human.update();
        assertTrue(human.x < 300, "ahora es un zombi y va a por el jugador");
    }

    @Test
    void zombieStillGettingUpDoesNotBite() {
        Npc human = NpcManager.spawn(NpcType.CIVILIAN, 60, 0, 0);
        human.die(false); // se convierte en zombi, pero tarda en levantarse
        human.update();
        assertEquals(100, Player.hp);
    }
}
