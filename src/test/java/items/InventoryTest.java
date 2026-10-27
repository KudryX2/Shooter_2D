package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void doesNotAcceptMoreThanItsCapacity() {
        Inventory inventory = new Inventory(2);
        assertTrue(inventory.add(Consumable.medkit()));
        assertTrue(inventory.add(Consumable.medkit()));
        assertTrue(inventory.isFull());
        assertFalse(inventory.add(Consumable.medkit()));
        assertEquals(2, inventory.size());
    }

    @Test
    void cyclingWrapsAroundAllSlots() {
        Inventory inventory = new Inventory(4);
        inventory.cycle(-1);
        assertEquals(3, inventory.selectedSlot());
        inventory.cycle(1);
        assertEquals(0, inventory.selectedSlot());
    }

    @Test
    void selectingAnEmptySlotLeavesTheHandEmpty() {
        Inventory inventory = new Inventory(4);
        inventory.add(Consumable.medkit());
        inventory.select(2);
        assertNull(inventory.selectedItem());
        assertNull(inventory.removeSelected());

        inventory.select(9); // no existe: se ignora
        assertEquals(2, inventory.selectedSlot());
    }

    @Test
    void removingTheSelectedItemShiftsTheRest() {
        Inventory inventory = new Inventory(4);
        Gun gun = new Gun(GunType.PISTOL);
        Consumable medkit = Consumable.medkit();
        inventory.add(gun);
        inventory.add(medkit);

        assertSame(gun, inventory.removeSelected());
        assertSame(medkit, inventory.selectedItem());
    }

    @Test
    void firstGunSkipsOtherItems() {
        Inventory inventory = new Inventory(4);
        Gun gun = new Gun(GunType.SNIPER);
        inventory.add(Consumable.medkit());
        inventory.add(gun);
        assertSame(gun, inventory.firstGun());
    }

    @Test
    void removeAllEmptiesAndReturnsEverything() {
        Inventory inventory = new Inventory(4);
        inventory.add(Consumable.medkit());
        inventory.add(new Gun(GunType.PISTOL));
        assertEquals(2, inventory.removeAll().size());
        assertTrue(inventory.isEmpty());
    }
}
