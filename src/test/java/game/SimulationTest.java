package game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import items.Projectile;
import npc.Npc;
import npc.NpcKind;
import player.Player;
import util.GameClock;
import world.WorldManager;

/** Simula partidas sin ventana para detectar bucles o crecimientos de memoria al matar zombis. */
class SimulationTest {

    private static void resetWorld() {
        WorldManager.reset();
        WorldManager.activeChunks.clear();
        WorldManager.projectiles.clear();
        WorldManager.npcs.clear();
        Player.inventory.removeAll();
        DemoWorld.create();
        Player.x = Player.SPAWN_X;
        Player.y = Player.SPAWN_Y;
        Player.hp = 100;
        Player.aimX = Float.NaN; // no apunta a nada: sin headshots salvo que el test lo pida
        Player.aimY = Float.NaN;
        GameClock.reset();
        GameClock.tick(8);
    }

    private static long runFrames(int frames) {
        long worst = 0;
        for (int i = 0; i < frames; i++) {
            GameClock.tick(8); // cada frame son 8 ms de juego
            long start = System.nanoTime();
            WorldManager.update();
            worst = Math.max(worst, System.nanoTime() - start);
            assertTrue(WorldManager.projectiles.size() < 2_000, "proyectiles sin control");
            assertTrue(WorldManager.npcs.size() < 500, "NPCs sin control");
        }
        return worst / 1_000_000;
    }

    @Test
    void killingZombiesByBulletsAndHeadshotsStaysStable() {
        resetWorld();
        DemoWorld.spawnWave(1200);
        runFrames(200);

        // Disparos del jugador contra cada zombi (daño normal)
        for (Npc npc : WorldManager.npcs) {
            if (npc.kind == NpcKind.ZOMBIE) {
                for (int i = 0; i < 5; i++) {
                    WorldManager.projectiles.add(new Projectile(npc.x - 30, npc.y, 0, 3, 600, 35, true));
                }
            }
        }
        long worstMs = runFrames(3_000);
        // Otros 24 s de juego (3.000 frames de 8 ms): los cadáveres desaparecen a los 4 s
        worstMs = Math.max(worstMs, runFrames(3_000));

        // Headshot directo
        for (Npc npc : WorldManager.npcs) {
            if (npc.isAlive()) npc.die(true);
        }
        worstMs = Math.max(worstMs, runFrames(2_000));

        System.out.println("Zombis abatidos: " + WorldManager.zombiesKilled + ", peor frame: " + worstMs + " ms"
                + ", NPCs restantes: " + WorldManager.npcs.size()
                + ", memoria usada: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_048_576 + " MB");
        assertTrue(WorldManager.zombiesKilled > 0);
        assertTrue(worstMs < 200, "frame demasiado lento: " + worstMs + " ms");
        assertFalse(WorldManager.npcs.stream().anyMatch(n -> !n.isAlive() && n.canDisappear()));
    }
}
