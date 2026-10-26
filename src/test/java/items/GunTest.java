package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.WorldManager;

class GunTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
        WorldManager.projectiles.clear();
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
