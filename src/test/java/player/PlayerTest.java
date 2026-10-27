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

import assets.TextureId;
import items.Consumable;
import items.Gun;
import items.GunType;
import structs.Struct;
import structs.StructKind;
import world.Chunk;
import world.WorldManager;

class PlayerTest {

    @BeforeEach
    void setUp() {
        Player.inventory.clear();
        Player.selectedCell = 0;
    }

    /** Cálculo de referencia: el algoritmo original, recorriendo todas las estructuras. */
    private static boolean[] bruteForceCollisions(List<Struct> structs, float x, float y) {
        float half = Player.SIZE / 2, m = 10;
        boolean up = false, down = false, left = false, right = false;
        for (Struct w : structs) {
            float sw = w.halfWidth(), sh = w.halfHeight();
            boolean overlapX = w.x() + sw > x - half && w.x() - sw < x + half;
            boolean overlapY = w.y() + sh > y - half && w.y() - sh < y + half;
            if (overlapX && w.y() + sh + m > y - half && w.y() - sh + m < y - half) up = true;
            if (overlapX && w.y() + sh - m > y + half && w.y() - sh - m < y + half) down = true;
            if (overlapY && w.x() + sw + m > x - half && w.x() - sw + m < x - half) left = true;
            if (overlapY && w.x() + sw - m > x + half && w.x() - sw - m < x + half) right = true;
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
            StructKind kind = random.nextBoolean() ? StructKind.WALL : StructKind.HALF_WALL;
            int tx = random.nextInt(15), ty = random.nextInt(15);
            Struct wall = switch (i % 3) {
                case 0 -> Struct.northWall(tx, ty, kind);
                case 1 -> Struct.westWall(tx, ty, kind);
                default -> new Struct(random.nextFloat() * 1000 - 100, random.nextFloat() * 1000 - 100, kind,
                        TextureId.WALL, random.nextBoolean());
            };
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
    void thinWallsBlockThePlayer() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        Struct wall = Struct.westWall(1, 0, StructKind.WALL); // vertical en x = 32.5
        WorldManager.build(wall);

        Player.y = 0;
        Player.x = wall.x() - wall.halfWidth() - Player.SIZE / 2 - 2; // pegado por la izquierda
        Player.checkCollisions();
        assertTrue(Player.collRight);
        assertFalse(Player.collLeft);

        Player.x = wall.x() + wall.halfWidth() + Player.SIZE / 2 + 2; // pegado por la derecha
        Player.checkCollisions();
        assertTrue(Player.collLeft);
        assertFalse(Player.collRight);
    }

    @Test
    void wallsCanOnlyBeBuiltOnFreeEdgesInRangeAndNotOnThePlayer() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        Player.x = 0;
        Player.y = -30; // casi sobre el borde norte de la casilla (0, 0), en y = -32.5
        WorldManager.build(Struct.northWall(2, 2, StructKind.WALL));

        assertFalse(Player.canBuild(Struct.northWall(2, 2, StructKind.HALF_WALL)), "ocupado");
        assertTrue(Player.canBuild(Struct.westWall(2, 2, StructKind.WALL)), "esquina con el otro");
        assertTrue(Player.canBuild(Struct.northWall(3, 2, StructKind.WALL)), "a continuación");
        assertFalse(Player.canBuild(Struct.northWall(0, 0, StructKind.WALL)), "encima del jugador");
        assertFalse(Player.canBuild(Struct.northWall(10, 0, StructKind.WALL)), "fuera de alcance");
        assertTrue(Player.canBuild(new Struct(0, 0, StructKind.FLOOR, TextureId.GRASS)), "suelo bajo el jugador");
    }

    @Test
    void demolishRemovesTheAimedWallWithinRange() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
        Player.x = 0;
        Player.y = 0;
        Struct wall = Struct.northWall(2, 2, StructKind.WALL);
        Struct next = Struct.northWall(3, 2, StructKind.HALF_WALL);
        Struct far = Struct.westWall(12, 0, StructKind.WALL);
        WorldManager.build(wall);
        WorldManager.build(next);
        WorldManager.build(far);

        assertTrue(Player.demolish(wall.x() + 5, wall.y() - 3)); // apuntando cerca del muro
        assertFalse(WorldManager.hasWallAt(wall));
        assertFalse(WorldManager.isBlocked(wall.x(), wall.y(), 1, 1), "ya no bloquea el paso");
        assertTrue(WorldManager.hasWallAt(next), "el de al lado sigue en pie");
        assertTrue(WorldManager.chunks.stream().noneMatch(c -> c.walls.contains(wall)), "ya no se dibuja");

        assertFalse(Player.demolish(far.x(), far.y()), "fuera de alcance");
        assertTrue(WorldManager.hasWallAt(far));

        assertTrue(Player.demolish(next.x(), next.y()), "también medios muros");
        assertFalse(WorldManager.hasWallAt(next));
    }

    @Test
    void demolishOnlyRemovesBuiltFloorsAndNeverTheMapGround() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        Player.x = 0;
        Player.y = 0;
        WorldManager.build(new Struct(65, 0, StructKind.FLOOR, TextureId.GRASS)); // suelo del mapa
        WorldManager.build(new Struct(65, 0, StructKind.FLOOR, TextureId.DIRT));  // construido encima

        assertTrue(Player.demolish(70, 5));
        assertEquals(1, WorldManager.chunks.getFirst().floors.size());
        assertEquals(TextureId.GRASS, WorldManager.chunks.getFirst().floors.getFirst().texture());
        assertFalse(Player.demolish(70, 5), "el suelo del mapa no se quita");
    }

    @Test
    void medkitHealsAndIsConsumed() {
        Player.hp = 30;
        Player.inventory.add(Consumable.medkit());

        assertTrue(Player.useSelectedConsumable());
        assertEquals(30 + Consumable.MEDKIT_HEAL, Player.hp);
        assertTrue(Player.inventory.isEmpty());
    }

    @Test
    void medkitNeverHealsAboveMaxHp() {
        Player.hp = 90;
        Player.inventory.add(Consumable.medkit());

        Player.useSelectedConsumable();
        assertEquals(Player.MAX_HP, Player.hp);
    }

    @Test
    void medkitIsNotWastedAtFullHealth() {
        Player.hp = Player.MAX_HP;
        Player.inventory.add(Consumable.medkit());

        assertFalse(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.size());
    }

    @Test
    void onlyTheSelectedSlotIsUsed() {
        Player.hp = 50;
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.inventory.add(Consumable.medkit());

        assertFalse(Player.useSelectedConsumable(), "la casilla 0 es una pistola");
        Player.selectedCell = 1;
        assertTrue(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.size());
    }
}
