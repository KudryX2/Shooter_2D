package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Geometry.dirX;
import static util.Geometry.dirY;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import assets.TextureId;
import items.Projectile;
import npc.Npc;
import npc.NpcManager;
import npc.NpcType;
import player.Player;
import structs.Struct;
import structs.StructKind;
import util.GameClock;

class ProjectilesTest {

    @BeforeEach
    void setUp() {
        GameClock.reset();
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        WorldManager.chunks.add(new Chunk(650, 0));
        Player.aimX = Float.NaN; // sin cursor: sin headshots salvo que el test lo pida
        Player.aimY = Float.NaN;
    }

    /** Dispara una bala quieta en (x, y) y dice si un muro la ha parado. */
    private static boolean bulletStoppedAt(float x, float y) {
        Player.x = 100_000; // lejos, para que la bala no le dé al jugador
        Player.y = 100_000;
        WorldManager.projectiles.clear();
        WorldManager.projectiles.add(new Projectile(x, y, 0, 0, 600, 1, false));
        WorldManager.update();
        return WorldManager.projectiles.isEmpty();
    }

    @Test
    void bulletsStopAtWallsButCrossHalfWallsAndFloors() {
        Structures.build(new Struct(100, 100, StructKind.WALL, TextureId.WALL));
        Structures.build(new Struct(400, 100, StructKind.HALF_WALL, TextureId.HALF_WALL));
        Structures.build(new Struct(700, 100, StructKind.FLOOR, TextureId.GRASS));

        // Muro horizontal fino: 80 x 16 px (una casilla más el grosor, para cerrar las esquinas)
        assertTrue(bulletStoppedAt(100, 100));
        assertTrue(bulletStoppedAt(139, 107));
        assertFalse(bulletStoppedAt(100, 109));
        assertFalse(bulletStoppedAt(141, 100));
        assertFalse(bulletStoppedAt(400, 100));
        assertFalse(bulletStoppedAt(700, 100));
    }

    /** Dispara una bala hacia +x desde (x, y) y avanza un frame en el que recorre {@code step} píxeles. */
    private static Projectile fireRight(float x, float y, float step) {
        Player.x = 100_000;
        Player.y = 100_000;
        WorldManager.projectiles.clear();
        Projectile p = new Projectile(x, y, 0, 1, 600, 50, false); // rotación 0 = hacia +x, 1 px/ms
        WorldManager.projectiles.add(p);
        GameClock.tick(step);
        WorldManager.update();
        return p;
    }

    @Test
    void fastBulletsCannotJumpOverWalls() {
        // Muro vertical de 16 px cruzado en el camino de una bala que avanza 300 px en un frame
        Structures.build(new Struct(100, 100, StructKind.WALL, TextureId.WALL, true));
        fireRight(0, 100, 300); // empieza antes del muro y acabaría muy detrás
        assertTrue(WorldManager.projectiles.isEmpty());
    }

    @Test
    void fastBulletsHitTheNpcTheyFlyThrough() {
        Npc target = NpcManager.spawn(NpcType.CIVILIAN, 100, 100, 0);
        fireRight(0, 100, 300);
        assertTrue(WorldManager.projectiles.isEmpty());
        assertTrue(target.hp < 100);
    }

    @Test
    void wallsProtectNpcsBehindThem() {
        Structures.build(new Struct(100, 100, StructKind.WALL, TextureId.WALL));
        Npc behind = NpcManager.spawn(NpcType.CIVILIAN, 200, 100, 0);
        fireRight(0, 100, 300); // el tramo cruza el muro y luego el NPC: gana el muro
        assertTrue(WorldManager.projectiles.isEmpty());
        assertEquals(100, behind.hp);
    }

    @Test
    void bulletsHitTheFirstNpcInTheirPath() {
        Npc near = NpcManager.spawn(NpcType.CIVILIAN, 100, 100, 0);
        Npc far = NpcManager.spawn(NpcType.CIVILIAN, 200, 100, 0);
        fireRight(0, 100, 300);
        assertTrue(near.hp < 100);
        assertEquals(100, far.hp);
    }

    /** Bala del jugador hacia +x a la altura (de suelo) {@code y}, con el cursor en (aimX, aimY). */
    private static void playerFiresRight(float y, float aimX, float aimY) {
        Player.x = 100_000;
        Player.y = 100_000;
        Player.aimX = aimX;
        Player.aimY = aimY;
        WorldManager.projectiles.add(new Projectile(0, y, 0, 1, 600, 50, true));
        GameClock.tick(300);
        WorldManager.update();
    }

    @Test
    void aimingAtTheHeadAsDrawnIsAHeadshot() {
        Npc target = NpcManager.spawn(NpcType.CIVILIAN, 100, 100, 0);
        playerFiresRight(100, 100, 100 - 54); // el cursor en la cabeza, que se dibuja ~54 px sobre los pies
        assertFalse(target.isAlive(), "headshot: un humano muere del todo, sin convertirse");
    }

    @Test
    void aimingAtTheBodyOnlyHurts() {
        Npc target = NpcManager.spawn(NpcType.CIVILIAN, 100, 100, 0);
        playerFiresRight(100, 100, 100 - 20);
        assertTrue(target.isAlive());
        assertTrue(target.hp < 100);
    }

    /** Como en el juego: apuntar con el cursor a la cabeza y disparar desde la mano. */
    @Test
    void anAimedShotFliesUnderTheCursorAndHitsTheHead() {
        Player.x = 0;
        Player.y = 100;
        Npc target = NpcManager.spawn(NpcType.CIVILIAN, 300, 100, 0);
        Player.aimAt(300, 100 - 54);
        float muzzleX = Player.x + dirX(Player.rotation) * Player.HAND_OFFSET;
        float muzzleY = Player.y + dirY(Player.rotation) * Player.HAND_OFFSET;
        WorldManager.projectiles.add(new Projectile(muzzleX, muzzleY, Player.rotation, 1, 600, 50, true));
        GameClock.tick(400);
        WorldManager.update();
        assertFalse(target.isAlive());
    }

    @Test
    void bulletsMissWallsBesideTheirPath() {
        Structures.build(new Struct(100, 100, StructKind.WALL, TextureId.WALL));
        fireRight(0, 110, 300); // pasa 10 px por debajo de un muro horizontal de 16 px de grosor
        assertEquals(1, WorldManager.projectiles.size());
    }

    @Test
    void bulletGridMatchesCheckingEveryWall() {
        // Muros fuera de la cuadrícula y en coordenadas negativas, como los que construye el jugador
        var random = new java.util.Random(7);
        var walls = new java.util.ArrayList<Struct>();
        for (int i = 0; i < 200; i++) {
            Struct wall = new Struct(random.nextFloat() * 1400 - 400, random.nextFloat() * 1400 - 400,
                    StructKind.WALL, TextureId.WALL, random.nextBoolean());
            walls.add(wall);
            Structures.build(wall);
        }
        for (int i = 0; i < 2_000; i++) {
            float x = random.nextFloat() * 1600 - 500;
            float y = random.nextFloat() * 1600 - 500;
            boolean expected = walls.stream().anyMatch(w -> w.overlaps(x, y, 0, 0));
            assertEquals(expected, bulletStoppedAt(x, y), "en " + x + "," + y);
        }
    }
}
