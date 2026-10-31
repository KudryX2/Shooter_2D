package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.GameClock;
import world.WorldManager;

class AmmoTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        GameClock.reset();
    }

    /** Dispara una vez como el jugador (con el gatillo recién pulsado) y deja pasar el tiempo entre disparos. */
    private static void playerShot(Gun gun) {
        gun.shoot(0, 0, 0, true, true);
        GameClock.tick(gun.type.rate + 1);
    }

    @Test
    void gunsComeWithAFullMagazineAndEachShotSpendsOneRound() {
        Gun pistol = new Gun(GunType.PISTOL);
        assertEquals(GunType.PISTOL.magazine, pistol.rounds());
        playerShot(pistol);
        assertEquals(GunType.PISTOL.magazine - 1, pistol.rounds());

        Gun shotgun = new Gun(GunType.SHOTGUN);
        playerShot(shotgun);
        assertEquals(GunType.SHOTGUN.magazine - 1, shotgun.rounds(), "todos los perdigones son una bala");
    }

    @Test
    void anEmptyMagazineDoesNotFire() {
        Gun sniper = new Gun(GunType.SNIPER);
        for (int i = 0; i < GunType.SNIPER.magazine; i++) {
            playerShot(sniper);
        }
        assertEquals(0, sniper.rounds());
        WorldManager.projectiles.clear();
        playerShot(sniper);
        assertTrue(WorldManager.projectiles.isEmpty());
    }

    @Test
    void npcsNeverRunOutOfAmmo() {
        Gun gun = new Gun(GunType.PISTOL);
        for (int i = 0; i < GunType.PISTOL.magazine * 2; i++) {
            gun.shoot(0, 0, 0, false, true);
            GameClock.tick(gun.type.rate * 2L + 1);
        }
        assertEquals(GunType.PISTOL.magazine, gun.rounds());
    }

    @Test
    void reloadingTakesItsTimeAndTakesRoundsFromTheEmptiestBoxFirst() {
        Inventory inventory = new Inventory(4);
        Gun ak = new Gun(GunType.ASSAULT_RIFLE);
        AmmoBox full = new AmmoBox(AmmoType.RIFLE, 50);
        AmmoBox almostEmpty = new AmmoBox(AmmoType.RIFLE, 4);
        inventory.add(ak);
        inventory.add(full);
        inventory.add(almostEmpty);
        inventory.add(new AmmoBox(AmmoType.PISTOL, 100)); // otra munición: no sirve
        for (int i = 0; i < 10; i++) {
            playerShot(ak);
        }

        assertTrue(ak.startReload(inventory));
        assertFalse(ak.startReload(inventory), "ya está recargando");
        GameClock.tick(GunType.ASSAULT_RIFLE.reloadMs / 2);
        assertFalse(ak.updateReload(inventory));
        assertEquals(0.5f, ak.reloadProgress(), 0.02f);
        GameClock.tick(GunType.ASSAULT_RIFLE.reloadMs / 2);
        assertTrue(ak.updateReload(inventory));

        assertEquals(GunType.ASSAULT_RIFLE.magazine, ak.rounds());
        assertNull(inventory.get(2), "la caja de 4 se ha gastado y desaparece");
        assertEquals(44, full.rounds(), "las otras 6, de la llena");
        assertEquals(100, inventory.totalRounds(AmmoType.PISTOL));
        assertEquals(-1, ak.reloadProgress());
    }

    @Test
    void cannotReloadAFullGunOrWithoutAmmo() {
        Inventory inventory = new Inventory(4);
        Gun pistol = new Gun(GunType.PISTOL);
        inventory.add(new AmmoBox(AmmoType.PISTOL, 10));
        assertFalse(pistol.startReload(inventory), "cargador lleno");

        playerShot(pistol);
        Inventory empty = new Inventory(4);
        assertFalse(pistol.startReload(empty), "sin cajas");
        empty.add(new AmmoBox(AmmoType.SHOTGUN, 10));
        assertFalse(pistol.startReload(empty), "cajas de otra munición");
    }

    @Test
    void doesNotFireWhileReloadingAndCancellingSpendsNothing() {
        Inventory inventory = new Inventory(4);
        Gun pistol = new Gun(GunType.PISTOL);
        AmmoBox box = new AmmoBox(AmmoType.PISTOL, 30);
        inventory.add(box);
        playerShot(pistol);
        pistol.startReload(inventory);

        WorldManager.projectiles.clear();
        playerShot(pistol);
        assertTrue(WorldManager.projectiles.isEmpty(), "recargando no dispara");

        pistol.cancelReload();
        GameClock.tick(GunType.PISTOL.reloadMs);
        assertFalse(pistol.updateReload(inventory));
        assertEquals(30, box.rounds());
        assertEquals(GunType.PISTOL.magazine - 1, pistol.rounds());
    }

    @Test
    void draggingABoxOntoAnotherOfTheSameAmmoFillsIt() {
        Inventory inventory = new Inventory(3);
        AmmoBox target = new AmmoBox(AmmoType.PISTOL, 80);
        inventory.add(new AmmoBox(AmmoType.PISTOL, 30));
        inventory.add(target);
        inventory.add(new AmmoBox(AmmoType.SNIPER, 10));

        inventory.move(0, 1);
        assertEquals(AmmoBox.CAPACITY, target.rounds());
        assertEquals(10, ((AmmoBox) inventory.get(0)).rounds(), "lo que no cabe se queda en la de origen");

        inventory.move(0, 1); // la de destino está llena: se intercambian, como cualquier objeto
        assertSame(target, inventory.get(0));

        Inventory merge = new Inventory(2);
        merge.add(new AmmoBox(AmmoType.RIFLE, 20));
        merge.add(new AmmoBox(AmmoType.RIFLE, 20));
        merge.move(0, 1);
        assertNull(merge.get(0), "vacía: desaparece");
        assertEquals(40, merge.totalRounds(AmmoType.RIFLE));

        inventory.move(2, 1); // otra munición: se intercambian
        assertTrue(inventory.get(1) instanceof AmmoBox box && box.type == AmmoType.SNIPER);
    }

    @Test
    void boxesNeverHoldMoreThanTheirCapacityAndDoNotStack() {
        assertEquals(AmmoBox.CAPACITY, new AmmoBox(AmmoType.PISTOL, 500).rounds());
        Inventory inventory = new Inventory(2);
        inventory.add(new AmmoBox(AmmoType.PISTOL, 10));
        inventory.add(new AmmoBox(AmmoType.PISTOL, 10));
        assertEquals(1, inventory.count(0));
        assertEquals(1, inventory.count(1), "cada caja en su casilla");
    }
}
