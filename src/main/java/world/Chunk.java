package world;

import java.util.ArrayList;
import java.util.List;

import items.DroppedItem;
import structs.Struct;

public final class Chunk {
    public final float x, y;
    public final List<DroppedItem> droppedItems = new ArrayList<>();

    public final List<Struct> walls = new ArrayList<>();     // colisionan con jugador, NPC y proyectiles
    public final List<Struct> halfWalls = new ArrayList<>(); // colisionan con jugador y NPC
    public final List<Struct> floors = new ArrayList<>();    // sin colisión

    public Chunk(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void add(Struct struct) {
        switch (struct.kind()) {
            case WALL -> walls.add(struct);
            case HALF_WALL -> halfWalls.add(struct);
            case FLOOR -> floors.add(struct);
            default -> throw new IllegalArgumentException("No es una estructura: " + struct.kind());
        }
    }
}
