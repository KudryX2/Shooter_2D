package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import structs.StructKind;
import util.GameClock;
import world.WorldManager;

class GunTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        GameClock.reset();
    }

    @Test
    void bulletsDeviateWithinEachGunsSpread() {
        for (GunType type : GunType.values()) {
            for (int i = 0; i < 50; i++) {
                WorldManager.projectiles.clear();
                Gun gun = new Gun(type); // arma nueva: sin tiempo de recarga
                gun.shoot(0, 0, 90, true, true);
                float center = type == GunType.SHOTGUN ? Gun.SHOTGUN_PATTERN[0] : 0; // primer perdigón del abanico
                float deviation = WorldManager.projectiles.getFirst().rotation - 90 - center;
                assertTrue(Math.abs(deviation) <= type.spread, type + " se desvía " + deviation);
            }
        }
    }

    @Test
    void sniperIsMoreAccurateThanPistolAndPistolMoreThanAutomatics() {
        assertTrue(GunType.SNIPER.spread < GunType.PISTOL.spread);
        assertTrue(GunType.PISTOL.spread < GunType.SHOTGUN.spread
                && GunType.PISTOL.spread < GunType.ASSAULT_RIFLE.spread);
    }

    @Test
    void eachShotLeavesOneShortMuzzleFlash() {
        Gun shotgun = new Gun(GunType.SHOTGUN);
        shotgun.shoot(0, 0, 0, true, true);
        assertEquals(1, WorldManager.muzzleFlashes.size(), "uno por disparo, no por perdigón");

        GameClock.tick(world.MuzzleFlash.DURATION_MS + 30);
        WorldManager.update();
        assertEquals(0, WorldManager.muzzleFlashes.size());
    }

    @Test
    void fireRateFollowsTheGameClock() {
        Gun ak = new Gun(GunType.ASSAULT_RIFLE); // cadencia: 50 ms
        ak.shoot(0, 0, 0, true, false);
        ak.shoot(0, 0, 0, true, false); // mismo instante: no dispara
        assertEquals(1, WorldManager.projectiles.size());

        GameClock.tick(51);
        ak.shoot(0, 0, 0, true, false);
        assertEquals(2, WorldManager.projectiles.size());
    }

    @Test
    void bulletsRememberWhoFiredThem() {
        new Gun(GunType.PISTOL).shoot(0, 0, 0, true, true);
        new Gun(GunType.PISTOL).shoot(0, 0, 0, false, true);
        assertTrue(WorldManager.projectiles.get(0).byPlayer);
        assertFalse(WorldManager.projectiles.get(1).byPlayer);
    }

    @Test
    void shotgunFiresFiveProjectiles() {
        Gun shotgun = new Gun(GunType.SHOTGUN);
        shotgun.shoot(0, 0, 0, true, true);
        assertEquals(5, WorldManager.projectiles.size());
    }

    @Test
    void semiAutomaticGunNeedsFreshTriggerPress() {
        Gun pistol = new Gun(GunType.PISTOL);
        pistol.shoot(0, 0, 0, true, false);
        assertEquals(0, WorldManager.projectiles.size());
    }

    @Test
    void automaticGunFiresWhileTriggerHeld() {
        Gun ak = new Gun(GunType.ASSAULT_RIFLE);
        ak.shoot(0, 0, 0, true, false);
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void cooldownPreventsImmediateSecondShot() {
        Gun pistol = new Gun(GunType.PISTOL);
        pistol.shoot(0, 0, 0, true, true);
        pistol.shoot(0, 0, 0, true, true);
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void eachGunHasItsOwnCooldown() {
        Gun a = new Gun(GunType.PISTOL);
        Gun b = new Gun(GunType.PISTOL);

        a.shoot(0, 0, 0, true, true);
        b.shoot(0, 0, 0, true, true);
        assertEquals(2, WorldManager.projectiles.size());
    }

    @Test
    void shootingPointBlankAtAWallDoesNotPutBulletsBehindIt() {
        WorldManager.chunks.add(new world.Chunk(0, 0));
        // Muro vertical en x = 32.5; el tirador está en x = 0 y la boca del arma, a 40 px, ya al otro lado
        WorldManager.build(structs.Struct.westWall(1, 0, StructKind.WALL));
        Gun shotgun = new Gun(GunType.SHOTGUN);
        shotgun.shoot(0, 0, 40, 0, 0, true, true);
        assertTrue(WorldManager.projectiles.isEmpty());
    }

    @Test
    void halfWallsDoNotStopPointBlankShots() {
        WorldManager.chunks.add(new world.Chunk(0, 0));
        WorldManager.build(structs.Struct.westWall(1, 0, StructKind.HALF_WALL));
        new Gun(GunType.PISTOL).shoot(0, 0, 40, 0, 0, true, true);
        assertEquals(1, WorldManager.projectiles.size());
    }
}
