package graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.Consumable;
import items.Gun;
import items.GunType;
import player.Player;
import structs.Struct;
import structs.StructKind;
import world.Chunk;
import world.Container;
import world.Structures;
import world.WorldManager;

class InventoryPanelTest {
    private static final int SLOTS = Player.HOTBAR_SLOTS + Player.BACKPACK_SLOTS;

    @BeforeEach
    void setUp() {
        HudRenderer.hudScale = DisplayManager.WIDTH / 100f; // como al arrancar
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        Player.inventory.removeAll();
        Player.x = 0;
        Player.y = 0;
        if (!InventoryPanel.isOpen()) {
            InventoryPanel.toggle();
        }
    }

    @AfterEach
    void tearDown() {
        if (InventoryPanel.isOpen()) {
            InventoryPanel.toggle();
        }
    }

    private static float centerX(int slot) {
        return InventoryLayout.slotX(slot) + InventoryLayout.cellSize() / 2;
    }

    private static float centerY(int slot) {
        return InventoryLayout.slotY(slot) + InventoryLayout.cellSize() / 2;
    }

    @Test
    void everySlotIsFoundAtItsCenterAndTheBackpackIsAboveTheHotbar() {
        for (int slot = 0; slot < SLOTS; slot++) {
            assertEquals(slot, InventoryLayout.slotAt(centerX(slot), centerY(slot), true, false), "casilla " + slot);
        }
        assertTrue(InventoryLayout.slotY(Player.HOTBAR_SLOTS) < InventoryLayout.slotY(SLOTS - 1), "filas en orden");
        assertTrue(InventoryLayout.slotY(SLOTS - 1) < InventoryLayout.slotY(0), "mochila encima de la barra");
        int backpack = Player.HOTBAR_SLOTS;
        assertEquals(-1, InventoryLayout.slotAt(centerX(backpack), centerY(backpack), false, false),
                "cerrado, la mochila no cuenta");
        assertTrue(InventoryLayout.slotY(Player.HOTBAR_SLOTS) > 0, "cabe en la pantalla");
    }

    @Test
    void theBackpackIsSeparatedFromTheHotbarByTheHotbarsHeight() {
        float backpackPanelBottom = InventoryLayout.backpackPanelTop() + InventoryLayout.backpackPanelHeight();
        float gap = InventoryLayout.hotbarPanelTop() - backpackPanelBottom;
        assertEquals(InventoryLayout.hotbarPanelHeight(), gap, 0.01f);
        assertTrue(InventoryLayout.panelContains(DisplayManager.WIDTH / 2f, backpackPanelBottom + gap / 2, false),
                "soltar en el hueco no tira nada");
    }

    @Test
    void draggingOntoAnotherSlotMovesTheStack() {
        Player.inventory.add(Consumable.medkit(), 3);
        int backpack = Player.HOTBAR_SLOTS + 5;

        InventoryPanel.startDrag(centerX(0), centerY(0));
        assertEquals(0, InventoryPanel.draggedSlot());
        InventoryPanel.release(centerX(backpack), centerY(backpack));

        assertNull(Player.inventory.get(0));
        assertEquals(3, Player.inventory.count(backpack));
        assertEquals(-1, InventoryPanel.draggedSlot());
    }

    @Test
    void draggingOntoAnOccupiedSlotSwapsThem() {
        Gun gun = new Gun(GunType.PISTOL);
        Player.inventory.add(gun);
        Player.inventory.add(Consumable.medkit());

        InventoryPanel.startDrag(centerX(0), centerY(0));
        InventoryPanel.release(centerX(1), centerY(1));

        assertTrue(Player.inventory.get(0) instanceof Consumable);
        assertSame(gun, Player.inventory.get(1));
    }

    @Test
    void droppingOutsideThePanelThrowsTheWholeStack() {
        Player.inventory.add(Consumable.medkit(), 4);

        InventoryPanel.startDrag(centerX(0), centerY(0));
        InventoryPanel.release(DisplayManager.WIDTH / 2f, 10); // arriba del todo, fuera del panel

        assertNull(Player.inventory.get(0));
        assertEquals(4, WorldManager.chunks.getFirst().droppedItems.getFirst().count());
    }

    @Test
    void droppingBetweenSlotsInsideThePanelDoesNothing() {
        Player.inventory.add(Consumable.medkit(), 2);
        float gapX = InventoryLayout.slotX(1) - InventoryLayout.pixel() / 2f; // la raya entre la casilla 0 y la 1

        InventoryPanel.startDrag(centerX(0), centerY(0));
        InventoryPanel.release(gapX, centerY(0));

        assertEquals(2, Player.inventory.count(0));
        assertTrue(WorldManager.chunks.getFirst().droppedItems.isEmpty());
    }

    @Test
    void emptySlotsCannotBeDraggedAndClosingCancelsADrag() {
        InventoryPanel.startDrag(centerX(3), centerY(3));
        assertEquals(-1, InventoryPanel.draggedSlot(), "vacía");

        Player.inventory.add(Consumable.medkit());
        InventoryPanel.startDrag(centerX(0), centerY(0));
        InventoryPanel.toggle();
        assertFalse(InventoryPanel.isOpen());
        assertEquals(-1, InventoryPanel.draggedSlot());
    }

    @Test
    void openingACrateOpensThePanelAndItemsCanBeDraggedBetweenBoth() {
        InventoryPanel.toggle(); // empieza cerrado
        Container crate = Structures.buildContainer(Struct.onTile(1, 0, StructKind.CRATE));
        crate.inventory.add(Consumable.medkit(), 2);
        Player.interact();
        InventoryPanel.followContainer();
        assertTrue(InventoryPanel.isOpen(), "se abre con la caja");

        int crateSlot = InventoryLayout.CONTAINER_FIRST;
        int backpack = Player.HOTBAR_SLOTS;
        assertTrue(InventoryLayout.slotY(crateSlot) < InventoryLayout.backpackPanelTop(), "encima de la mochila");
        assertTrue(InventoryLayout.slotY(crateSlot) > 0, "cabe en la pantalla");
        InventoryPanel.startDrag(centerX(crateSlot), centerY(crateSlot));
        InventoryPanel.release(centerX(backpack), centerY(backpack));
        assertNull(crate.inventory.get(0));
        assertEquals(2, Player.inventory.count(backpack));

        Player.closeContainer(); // p.ej. al alejarse
        InventoryPanel.followContainer();
        assertFalse(InventoryPanel.isOpen(), "se cierra con la caja");
    }

    @Test
    void shiftRightClickSendsAStackToTheOtherSide() {
        Container crate = Structures.buildContainer(Struct.onTile(1, 0, StructKind.CRATE));
        crate.inventory.add(Consumable.medkit(), 3);
        Player.inventory.add(new Gun(GunType.PISTOL));

        InventoryPanel.quickMove(centerX(0), centerY(0)); // sin caja abierta: nada
        assertTrue(Player.inventory.get(0) instanceof Gun);

        Player.interact();
        int crateSlot = InventoryLayout.CONTAINER_FIRST;
        InventoryPanel.quickMove(centerX(crateSlot), centerY(crateSlot));
        assertNull(crate.inventory.get(0));
        assertEquals(3, Player.inventory.count(1), "de la caja al inventario");

        InventoryPanel.quickMove(centerX(0), centerY(0));
        assertNull(Player.inventory.get(0));
        assertTrue(crate.inventory.get(0) instanceof Gun, "y del inventario a la caja");
        Player.closeContainer();
    }
}
