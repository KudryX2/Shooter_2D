package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Geometry.distance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import items.AmmoBox;
import items.AmmoType;
import items.Consumable;
import items.DroppedItem;
import items.Gun;
import items.GunType;
import structs.Struct;
import structs.StructKind;

class ContainerTest {

    @BeforeEach
    void setUp() {
        WorldManager.reset();
        WorldManager.chunks.add(new Chunk(0, 0));
    }

    @Test
    void cratesBlockWalkingButNotBullets() {
        Container crate = Structures.buildContainer(Struct.onTile(2, 0, StructKind.CRATE));
        float x = crate.struct.x();
        assertTrue(Structures.isSolidAt(x, 0));
        assertFalse(Structures.isWallBetween(0, 0, x + 100, 0), "las balas pasan por encima");
        assertSame(crate, Structures.containerAt(x + 10, 5), "cualquier punto de su casilla");
        assertSame(crate, Structures.nearestContainer(x - 60, 0, 80));
        assertNull(Structures.nearestContainer(x - 100, 0, 80), "demasiado lejos");
    }

    @Test
    void breakingACrateScattersWhatItHadAroundIt() {
        Container crate = Structures.buildContainer(Struct.onTile(0, 0, StructKind.CRATE));
        crate.inventory.add(Consumable.medkit(), 3);
        crate.inventory.add(new Gun(GunType.PISTOL));
        crate.inventory.add(new AmmoBox(AmmoType.RIFLE, 40));

        Structures.remove(crate);

        assertTrue(crate.isRemoved());
        assertNull(Structures.containerAt(0, 0));
        assertFalse(Structures.isSolidAt(0, 0), "ya no choca");
        var dropped = WorldManager.chunks.getFirst().droppedItems;
        assertEquals(3, dropped.size(), "cada pila en su montón");
        assertEquals(3, dropped.getFirst().count(), "la pila entera");
        for (DroppedItem d : dropped) {
            float r = distance(d.x(), d.y(), 0, 0);
            assertTrue(r >= Structures.SCATTER_RADIUS - 1 && r <= Structures.SCATTER_RADIUS * 1.5f + 1,
                    "alrededor de donde estaba: " + r);
        }
        assertTrue(distance(dropped.get(0).x(), dropped.get(0).y(), dropped.get(1).x(), dropped.get(1).y()) > 10,
                "no amontonadas en el mismo punto");
    }
}
