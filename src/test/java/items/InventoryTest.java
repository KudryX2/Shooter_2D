package items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import structs.StructKind;

class InventoryTest {

    @Test
    void doesNotAcceptMoreThanItsCapacity() {
        Inventory inventory = new Inventory(2);
        assertTrue(inventory.add(new Gun(GunType.PISTOL)));
        assertTrue(inventory.add(new Gun(GunType.SNIPER)));
        assertFalse(inventory.canAdd(new Gun(GunType.PISTOL)));
        assertFalse(inventory.add(new Gun(GunType.PISTOL)));
    }

    @Test
    void stackableItemsShareASlotUpToTheirMaximum() {
        Inventory inventory = new Inventory(3);
        assertTrue(inventory.add(Consumable.medkit()));
        assertTrue(inventory.add(Consumable.medkit()));
        assertEquals(2, inventory.count(0));
        assertNull(inventory.get(1), "la segunda va a la misma pila");

        int left = inventory.add(new StructItem(StructKind.WALL), StructItem.MAX_STACK + 5);
        assertEquals(0, left);
        assertEquals(StructItem.MAX_STACK, inventory.count(1), "pila llena");
        assertEquals(5, inventory.count(2), "y lo que sobra, a otra");
    }

    @Test
    void differentMaterialsDoNotStack() {
        Inventory inventory = new Inventory(2);
        inventory.add(new StructItem(StructKind.WALL));
        inventory.add(new StructItem(StructKind.HALF_WALL));
        assertEquals(1, inventory.count(0));
        assertEquals(1, inventory.count(1));
    }

    @Test
    void whatDoesNotFitIsReturned() {
        Inventory inventory = new Inventory(1);
        assertEquals(3, inventory.add(Consumable.medkit(), Consumable.MAX_STACK + 3));
        assertEquals(Consumable.MAX_STACK, inventory.count(0));
        assertFalse(inventory.canAdd(Consumable.medkit()), "la pila está llena y no hay huecos");
    }

    @Test
    void nonStackableItemsCannotBeAddedInBulk() {
        Inventory inventory = new Inventory(4);
        assertThrows(IllegalArgumentException.class, () -> inventory.add(new Gun(GunType.PISTOL), 2));
    }

    @Test
    void stacksFillUpBeforeTakingEmptySlots() {
        Inventory inventory = new Inventory(4, 2);
        inventory.add(new Gun(GunType.PISTOL));
        inventory.add(new Gun(GunType.SNIPER));
        inventory.add(Consumable.medkit()); // barra llena: a la mochila
        assertEquals(1, inventory.count(2));
        inventory.removeOne(0); // hueco en la barra
        inventory.add(Consumable.medkit());
        assertEquals(2, inventory.count(2), "completa la pila de la mochila antes que ocupar el hueco");
        assertNull(inventory.get(0));
    }

    @Test
    void removingLeavesAGapAndNothingMoves() {
        Inventory inventory = new Inventory(4);
        Gun gun = new Gun(GunType.PISTOL);
        Consumable medkit = Consumable.medkit();
        inventory.add(gun);
        inventory.add(medkit);

        assertSame(gun, inventory.removeSelected());
        assertNull(inventory.selectedItem());
        assertSame(medkit, inventory.get(1), "sigue en su casilla");
    }

    @Test
    void removingFromAStackTakesOneAtATime() {
        Inventory inventory = new Inventory(2);
        inventory.add(Consumable.medkit(), 2);
        assertTrue(inventory.removeOne(0) instanceof Consumable);
        assertEquals(1, inventory.count(0));
        inventory.removeOne(0);
        assertNull(inventory.get(0));
        assertEquals(0, inventory.count(0));
        assertNull(inventory.removeOne(0));
    }

    @Test
    void movingSwapsOrMergesStacks() {
        Inventory inventory = new Inventory(3);
        Gun gun = new Gun(GunType.PISTOL);
        inventory.add(gun);
        inventory.add(Consumable.medkit(), 4);
        inventory.move(0, 1);
        assertTrue(inventory.get(0) instanceof Consumable, "intercambiadas");
        assertSame(gun, inventory.get(1));

        inventory.move(0, 2); // a un hueco
        assertNull(inventory.get(0));
        assertEquals(4, inventory.count(2));

        // Dos pilas del mismo objeto: la de la casilla 1 con sitio para 2 más y la de la 0 con 4
        Inventory stacks = new Inventory(2);
        stacks.add(Consumable.medkit(), Consumable.MAX_STACK);
        stacks.move(0, 1);
        stacks.add(Consumable.medkit(), 4);
        stacks.removeOne(1);
        stacks.removeOne(1);
        stacks.move(0, 1);
        assertEquals(Consumable.MAX_STACK, stacks.count(1), "la completa");
        assertEquals(2, stacks.count(0), "y lo que no cabe se queda donde estaba");

        stacks.move(0, 1); // la de destino está llena: se intercambian
        assertEquals(Consumable.MAX_STACK, stacks.count(0));
        assertEquals(2, stacks.count(1));
    }

    @Test
    void selectionStaysInTheHotbar() {
        Inventory inventory = new Inventory(10, 4);
        inventory.cycle(-1);
        assertEquals(3, inventory.selectedSlot());
        inventory.cycle(1);
        assertEquals(0, inventory.selectedSlot());
        inventory.select(6); // es de la mochila: se ignora
        assertEquals(0, inventory.selectedSlot());
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
    void removeAllEmptiesAndReturnsTheStacks() {
        Inventory inventory = new Inventory(4);
        inventory.add(Consumable.medkit(), 3);
        inventory.add(new Gun(GunType.PISTOL));
        var all = inventory.removeAll();
        assertEquals(2, all.size());
        assertEquals(3, all.getFirst().count());
        assertTrue(inventory.isEmpty());
    }
}
