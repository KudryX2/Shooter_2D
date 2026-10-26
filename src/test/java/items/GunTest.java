package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.GameClock;
import world.WorldManager;

class GunTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
        WorldManager.reset();
        GameClock.reset();
    }

    @Test
    void bulletsDeviateWithinEachGunsSpread() {
        for (Gun template : ItemManager.guns) {
            for (int i = 0; i < 50; i++) {
                WorldManager.projectiles.clear();
                Gun gun = template.copy(); // copia nueva: sin tiempo de recarga
                gun.shoot(0, 0, 90, true, true);
                float center = template.gunKind == GunKind.SHOTGUN ? 12 : 0; // primer perdigón del abanico
                float deviation = WorldManager.projectiles.getFirst().rotation - 90 - center;
                assertTrue(Math.abs(deviation) <= template.spread, template.name + " se desvía " + deviation);
            }
        }
    }

    @Test
    void sniperIsMoreAccurateThanPistolAndPistolMoreThanAutomatics() {
        Gun pistol = ItemManager.guns.get(0), shotgun = ItemManager.guns.get(1);
        Gun sniper = ItemManager.guns.get(2), ak = ItemManager.guns.get(3);
        assertTrue(sniper.spread < pistol.spread);
        assertTrue(pistol.spread < shotgun.spread && pistol.spread < ak.spread);
    }

    @Test
    void eachShotLeavesOneShortMuzzleFlash() {
        Gun shotgun = ItemManager.guns.get(1).copy();
        shotgun.shoot(0, 0, 0, true, true);
        assertEquals(1, WorldManager.muzzleFlashes.size(), "uno por disparo, no por perdigón");

        GameClock.tick(world.MuzzleFlash.DURATION_MS + 30);
        WorldManager.update();
        assertEquals(0, WorldManager.muzzleFlashes.size());
    }

    @Test
    void fireRateFollowsTheGameClock() {
        Gun ak = ItemManager.guns.get(3).copy(); // cadencia: 50 ms
        ak.shoot(0, 0, 0, true, false);
        ak.shoot(0, 0, 0, true, false); // mismo instante: no dispara
        assertEquals(1, WorldManager.projectiles.size());

        GameClock.tick(51);
        ak.shoot(0, 0, 0, true, false);
        assertEquals(2, WorldManager.projectiles.size());
    }

    @Test
    void bulletsRememberWhoFiredThem() {
        ItemManager.guns.getFirst().copy().shoot(0, 0, 0, true, true);
        ItemManager.guns.getFirst().copy().shoot(0, 0, 0, false, true);
        assertTrue(WorldManager.projectiles.get(0).byPlayer);
        assertFalse(WorldManager.projectiles.get(1).byPlayer);
    }

    @Test
    void shotgunFiresFiveProjectiles() {
        Gun shotgun = ItemManager.guns.get(1).copy();
        shotgun.shoot(0, 0, 0, true, true);
        assertEquals(5, WorldManager.projectiles.size());
    }

    @Test
    void semiAutomaticGunNeedsFreshTriggerPress() {
        Gun pistol = ItemManager.guns.getFirst().copy();
        pistol.shoot(0, 0, 0, true, false);
        assertEquals(0, WorldManager.projectiles.size());
    }

    @Test
    void automaticGunFiresWhileTriggerHeld() {
        Gun ak = ItemManager.guns.get(3).copy();
        ak.shoot(0, 0, 0, true, false);
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void cooldownPreventsImmediateSecondShot() {
        Gun pistol = ItemManager.guns.getFirst().copy();
        pistol.shoot(0, 0, 0, true, true);
        pistol.shoot(0, 0, 0, true, true);
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void copiesHaveIndependentCooldowns() {
        Gun template = ItemManager.guns.getFirst();
        Gun a = template.copy();
        Gun b = template.copy();
        assertNotSame(a, b);

        a.shoot(0, 0, 0, true, true);
        b.shoot(0, 0, 0, true, true);
        assertEquals(2, WorldManager.projectiles.size());
    }
}
