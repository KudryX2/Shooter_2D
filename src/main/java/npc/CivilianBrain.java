package npc;

/** Huye de los zombis cercanos; si no hay ninguno, se queda quieto. */
final class CivilianBrain implements NpcBrain {
    private static final float FLEE_RANGE = 500;

    @Override
    public void update(Npc npc) {
        Npc zombie = NpcBrain.nearestZombie(npc, FLEE_RANGE);
        if (zombie != null) {
            npc.lookAt(zombie.x, zombie.y);
            npc.rotation += 180;
            npc.walk(1);
        }
    }
}
