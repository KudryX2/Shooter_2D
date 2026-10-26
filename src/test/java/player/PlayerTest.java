package player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.ItemManager;

class PlayerTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
        Player.inventory.clear();
        Player.selectedCell = 0;
    }

    @Test
    void medkitHealsAndIsConsumed() {
        Player.hp = 30;
        Player.inventory.add(ItemManager.medkit());

        assertTrue(Player.useSelectedConsumable());
        assertEquals(30 + ItemManager.MEDKIT_HEAL, Player.hp);
        assertTrue(Player.inventory.isEmpty());
    }

    @Test
    void medkitNeverHealsAboveMaxHp() {
        Player.hp = 90;
        Player.inventory.add(ItemManager.medkit());

        Player.useSelectedConsumable();
        assertEquals(Player.MAX_HP, Player.hp);
    }

    @Test
    void medkitIsNotWastedAtFullHealth() {
        Player.hp = Player.MAX_HP;
        Player.inventory.add(ItemManager.medkit());

        assertFalse(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.size());
    }

    @Test
    void onlyTheSelectedSlotIsUsed() {
        Player.hp = 50;
        Player.inventory.add(ItemManager.guns.getFirst().copy());
        Player.inventory.add(ItemManager.medkit());

        assertFalse(Player.useSelectedConsumable(), "la casilla 0 es una pistola");
        Player.selectedCell = 1;
        assertTrue(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.size());
    }
}
