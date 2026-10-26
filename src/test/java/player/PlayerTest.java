package player;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.ItemKind;
import items.ItemManager;
import structs.Struct;
import world.Chunk;
import world.WorldManager;

class PlayerTest {

    @BeforeEach
    void setUp() {
        ItemManager.init();
        Player.inventory.clear();
        Player.selectedCell = 0;
    }

    /** Cálculo de referencia: el algoritmo original, recorriendo todas las estructuras. */
    private static boolean[] bruteForceCollisions(List<Struct> structs, float x, float y) {
        float half = Player.SIZE / 2, s = Struct.SIZE / 2, m = 10;
        boolean up = false, down = false, left = false, right = false;
        for (Struct w : structs) {
            boolean overlapX = w.x() + s > x - half && w.x() - s < x + half;
            boolean overlapY = w.y() + s > y - half && w.y() - s < y + half;
            if (overlapX && w.y() + s + m > y - half && w.y() - s + m < y - half) up = true;
            if (overlapX && w.y() + s - m > y + half && w.y() - s - m < y + half) down = true;
            if (overlapY && w.x() + s + m > x - half && w.x() - s + m < x - half) left = true;
            if (overlapY && w.x() + s - m > x + half && w.x() - s - m < x + half) right = true;
        }
        return new boolean[] {up, down, left, right};
    }

    @Test
    void wallCollisionsMatchCheckingEveryWall() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        Random random = new Random(3);
        List<Struct> solids = new ArrayList<>();
        // Cuadrícula (como los mapas) y posiciones sueltas (como lo que construye el jugador)
        for (int i = 0; i < 150; i++) {
            ItemKind kind = random.nextBoolean() ? ItemKind.WALL : ItemKind.HALF_WALL;
            float wx = i % 2 == 0 ? random.nextInt(15) * Struct.SIZE : random.nextFloat() * 1000 - 100;
            float wy = i % 2 == 0 ? random.nextInt(15) * Struct.SIZE : random.nextFloat() * 1000 - 100;
            Struct wall = new Struct(wx, wy, kind, 2);
            solids.add(wall);
            WorldManager.build(wall);
        }

        int blockedSomewhere = 0;
        for (int i = 0; i < 10_000; i++) {
            Player.x = random.nextFloat() * 1100 - 150;
            Player.y = random.nextFloat() * 1100 - 150;
            Player.checkCollisions();
            boolean[] expected = bruteForceCollisions(solids, Player.x, Player.y);
            boolean[] actual = {Player.collUp, Player.collDown, Player.collLeft, Player.collRight};
            assertArrayEquals(expected, actual, "en " + Player.x + "," + Player.y);
            if (actual[0] || actual[1] || actual[2] || actual[3]) blockedSomewhere++;
        }
        assertTrue(blockedSomewhere > 500, "el test debería probar muchas posiciones pegadas a muros");
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
