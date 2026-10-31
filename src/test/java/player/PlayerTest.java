package player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.AmmoBox;
import items.AmmoType;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.GunType;
import items.Projectile;
import items.StructItem;
import structs.Struct;
import structs.StructKind;
import util.GameClock;
import world.Chunk;
import world.Container;
import world.Structures;
import world.WorldManager;

class PlayerTest {
    private static final float HALF = Player.SIZE / 2;

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
        GameClock.reset();
        Player.inventory.removeAll();
        Player.inventory.select(0);
        Player.x = 0;
        Player.y = 0;
        Player.hp = Player.MAX_HP;
    }

    /** Anda {@code frames} frames de 16 ms en la dirección (dx, dy). */
    private static void walk(int dx, int dy, int frames) {
        for (int i = 0; i < frames; i++) {
            GameClock.tick(16);
            Player.move(dx, dy);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Movimiento
    // ---------------------------------------------------------------------------------------------

    @Test
    void walksFreelyWithoutWalls() {
        walk(1, 0, 10);
        assertEquals(10 * 16 * 0.5f, Player.x, 0.01f);
        assertEquals(0, Player.y);
    }

    @Test
    void stopsRightAtAThinWall() {
        Struct wall = Struct.westWall(1, 0, StructKind.WALL); // vertical en x = 32.5, 16 px de grosor
        Structures.build(wall);

        walk(1, 0, 50);
        float touching = wall.x() - wall.halfWidth() - HALF;
        assertTrue(Player.x <= touching && Player.x > touching - 8, "se para pegado al muro: " + Player.x);
    }

    @Test
    void halfWallsAlsoBlockThePlayer() {
        Structures.build(Struct.westWall(1, 0, StructKind.HALF_WALL));
        walk(1, 0, 50);
        assertTrue(Player.x < 32.5f);
    }

    @Test
    void slidesAlongAWallWhenWalkingDiagonally() {
        Structures.build(Struct.westWall(1, 0, StructKind.WALL));
        Structures.build(Struct.westWall(1, 1, StructKind.WALL));
        walk(1, 1, 10);
        assertTrue(Player.x < 32.5f, "no atraviesa el muro");
        assertEquals(10 * 16 * 0.5f, Player.y, 0.01f, "pero sigue avanzando a lo largo de él");
    }

    @Test
    void aSlowFrameDoesNotGoThroughAThinWall() {
        Structures.build(Struct.westWall(1, 0, StructKind.WALL));
        GameClock.tick(500); // un frame de medio segundo: 250 px de golpe
        Player.move(1, 0);
        assertTrue(Player.x < 32.5f, "ha atravesado el muro: " + Player.x);
    }

    @Test
    void canWalkOutOfAWallBuiltOnTop() {
        Structures.build(Struct.westWall(0, 0, StructKind.WALL)); // en x = -32.5, tocando al jugador
        Player.x = -32.5f;
        walk(1, 0, 10);
        assertTrue(Player.x > -32.5f);
    }

    // ---------------------------------------------------------------------------------------------
    // Puntería y disparo
    // ---------------------------------------------------------------------------------------------

    @Test
    void looksAndShootsTowardsTheAimedPoint() {
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.aimAt(0, 300); // al sur
        assertEquals(90, Player.rotation, 0.01f);

        Player.primaryAction(true);
        Projectile bullet = WorldManager.projectiles.getFirst();
        assertEquals(0, bullet.x, 0.01f);
        assertEquals(Player.HAND_OFFSET, bullet.y, 0.01f, "sale de la mano, hacia el sur");
    }

    // ---------------------------------------------------------------------------------------------
    // Botiquines
    // ---------------------------------------------------------------------------------------------

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
        Player.inventory.add(Consumable.medkit());

        assertFalse(Player.useSelectedConsumable());
        assertEquals(1, Player.inventory.count(0));
    }

    @Test
    void onlyTheSelectedSlotIsUsed() {
        Player.hp = 50;
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.inventory.add(Consumable.medkit());

        assertFalse(Player.useSelectedConsumable(), "la casilla 0 es una pistola");
        Player.inventory.select(1);
        assertTrue(Player.useSelectedConsumable());
        assertNull(Player.inventory.get(1), "gastado");
        assertTrue(Player.inventory.get(0) instanceof Gun, "la pistola sigue ahí");
    }

    @Test
    void fillsTheHotbarFirstAndThenTheBackpack() {
        WorldManager.activeChunks.addAll(WorldManager.chunks);
        int slots = Player.HOTBAR_SLOTS + Player.BACKPACK_SLOTS;
        for (int i = 0; i <= slots; i++) {
            WorldManager.dropItem(new Gun(GunType.PISTOL), 0, 0);
        }
        for (int i = 0; i <= slots; i++) {
            Player.pickUpItem();
        }
        assertTrue(Player.inventory.get(Player.HOTBAR_SLOTS - 1) instanceof Gun, "barra rápida llena");
        assertTrue(Player.inventory.get(slots - 1) instanceof Gun, "y la mochila también");
        assertEquals(1, WorldManager.chunks.getFirst().droppedItems.size(), "el que sobra se queda en el suelo");
    }

    @Test
    void picksUpPilesAndLeavesWhatDoesNotFit() {
        WorldManager.activeChunks.addAll(WorldManager.chunks);
        for (int slot = 0; slot < Player.inventory.capacity() - 1; slot++) {
            Player.inventory.add(new Gun(GunType.PISTOL)); // solo queda una casilla libre
        }
        WorldManager.dropItem(Consumable.medkit(), Consumable.MAX_STACK + 4, 0, 0);

        Player.pickUpItem();
        assertEquals(Consumable.MAX_STACK, Player.inventory.count(Player.inventory.capacity() - 1));
        DroppedItem left = WorldManager.chunks.getFirst().droppedItems.getFirst();
        assertEquals(4, left.count(), "lo que no cabe se queda en el suelo");
    }

    @Test
    void droppingLeavesOneOnTheGround() {
        Player.inventory.add(Consumable.medkit(), 3);
        Player.dropSelectedItem();
        assertEquals(2, Player.inventory.count(0));
        assertEquals(1, WorldManager.chunks.getFirst().droppedItems.getFirst().count());
    }

    @Test
    void buildingSpendsMaterialAndDemolishingGivesItBack() {
        Player.inventory.add(new StructItem(StructKind.WALL), 2);
        Player.aimAt(Struct.SIZE * 2, Struct.SIZE / 2f - Projectile.FLIGHT_HEIGHT); // borde de casilla al este
        Player.updateBuildPreview();
        Player.primaryAction(true);
        assertEquals(1, Player.inventory.count(0), "gasta uno");

        Player.secondaryAction(); // lo derriba
        assertEquals(2, Player.inventory.count(0), "y lo recupera");
    }

    @Test
    void dyingDropsEverythingAndRespawnsWithFullHealth() {
        Player.inventory.add(new Gun(GunType.PISTOL));
        Player.inventory.add(Consumable.medkit(), 3);
        Player.hp = 0;

        Player.update();
        assertTrue(Player.inventory.isEmpty());
        assertEquals(2, WorldManager.chunks.getFirst().droppedItems.size());
        assertEquals(3, WorldManager.chunks.getFirst().droppedItems.get(1).count(), "la pila entera");
        assertEquals(Player.MAX_HP, Player.hp);
        assertEquals(Player.spawnX(), Player.x);
    }

    // ---------------------------------------------------------------------------------------------
    // Recarga
    // ---------------------------------------------------------------------------------------------

    @Test
    void reloadingFillsTheGunInHandFromItsAmmoBoxes() {
        Gun pistol = new Gun(GunType.PISTOL);
        Player.inventory.add(pistol);
        Player.inventory.add(new AmmoBox(AmmoType.PISTOL, 20));
        pistol.shoot(0, 0, 0, true, true);

        Player.reload();
        assertEquals(0, Player.reloadProgress(), 0.01f);
        GameClock.tick(GunType.PISTOL.reloadMs);
        Player.update();

        assertEquals(GunType.PISTOL.magazine, pistol.rounds());
        assertEquals(19, Player.inventory.totalRounds(AmmoType.PISTOL));
        assertEquals(-1, Player.reloadProgress(), "ya no recarga");
    }

    @Test
    void switchingWeaponsCancelsTheReloadWithoutSpendingAmmo() {
        Gun pistol = new Gun(GunType.PISTOL);
        Player.inventory.add(pistol);
        Player.inventory.add(new AmmoBox(AmmoType.PISTOL, 20));
        pistol.shoot(0, 0, 0, true, true);

        Player.reload();
        Player.inventory.select(1);
        Player.update();
        assertEquals(-1, Player.reloadProgress());
        GameClock.tick(GunType.PISTOL.reloadMs);
        Player.inventory.select(0);
        Player.update();

        assertEquals(GunType.PISTOL.magazine - 1, pistol.rounds());
        assertEquals(20, Player.inventory.totalRounds(AmmoType.PISTOL));
    }

    // ---------------------------------------------------------------------------------------------
    // Contenedores
    // ---------------------------------------------------------------------------------------------

    @Test
    void interactUsesWhateverIsCloserTheCrateOrWhatIsOnTheGround() {
        WorldManager.activeChunks.addAll(WorldManager.chunks);
        WorldManager.dropItem(Consumable.medkit(), 0, 0); // a los pies
        Container crate = Structures.buildContainer(Struct.onTile(1, 0, StructKind.CRATE)); // a 64

        Player.interact();
        assertNull(Player.openContainer(), "el botiquín está más cerca");
        assertTrue(Player.inventory.get(0) instanceof Consumable);

        Player.interact(); // ya no queda nada que coger
        assertSame(crate, Player.openContainer());

        Player.closeContainer();
        WorldManager.dropItem(Consumable.medkit(), 30, 0); // entre el jugador y la caja: más cerca que ella
        Player.interact();
        assertNull(Player.openContainer());
        assertEquals(2, Player.inventory.count(0));
    }

    @Test
    void anOpenCrateClosesWhenWalkingAwayOrWhenItBreaks() {
        Container crate = Structures.buildContainer(Struct.onTile(1, 0, StructKind.CRATE));
        Player.interact();
        Player.x = -Player.CLOSE_RANGE; // ya está a más de CLOSE_RANGE del centro de la caja
        Player.update();
        assertNull(Player.openContainer());

        Player.x = 0;
        Player.interact();
        Structures.remove(crate);
        Player.update();
        assertNull(Player.openContainer());
    }

    @Test
    void buildingACrateSpendsOneAndBreakingItGivesItBackAndScattersItsContents() {
        WorldManager.activeChunks.addAll(WorldManager.chunks);
        Player.inventory.add(new StructItem(StructKind.CRATE), 2);
        Player.aimAt(Struct.SIZE * 2, -Projectile.FLIGHT_HEIGHT); // casilla (2, 0)
        Player.updateBuildPreview();
        Player.primaryAction(true);
        assertEquals(1, Player.inventory.count(0), "gasta una");
        Container crate = Structures.containerAt(Struct.SIZE * 2, 0);
        crate.inventory.add(Consumable.medkit(), 2);

        Player.secondaryAction(); // la rompe
        assertEquals(2, Player.inventory.count(0), "la recupera");
        assertNull(Structures.containerAt(Struct.SIZE * 2, 0));
        assertEquals(2, WorldManager.chunks.getFirst().droppedItems.getFirst().count(), "y lo de dentro, al suelo");
    }
}
