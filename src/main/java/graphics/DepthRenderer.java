package graphics;

import static graphics.Camera.isVisible;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import npc.Npc;
import player.Player;
import structs.Struct;
import world.Chunk;
import world.WorldManager;

/**
 * Todo lo que tiene altura (muros, sacos terreros, NPC en pie y el jugador), de norte a sur según dónde tiene el
 * pie: lo que está más al sur tapa a lo que tiene detrás.
 *
 * <p>Para no perder de vista a nadie, los muros se aclaran: los que están delante del jugador en un radio a su
 * alrededor (con degradado hacia fuera) y cualquiera que tape a un NPC en pie.
 */
final class DepthRenderer {
    /** Opacidad de un muro que tapa a alguien. */
    static final float SEE_THROUGH_ALPHA = 0.45f;
    /** Medio lado de la parte visible de un personaje (el sprite mide 100 px, pero el cuerpo unos 44). */
    static final float VISIBLE_HALF = 24;
    /** Hasta esta distancia del jugador, los muros que tiene delante se ven del todo semitransparentes... */
    static final float FADE_NEAR = Struct.SIZE;
    /** ...y a partir de aquí ya son opacos (entre medias, degradado). */
    static final float FADE_FAR = Struct.SIZE * 2.5f;

    private record Entry(float depth, Object thing) {
    }

    private static final Object PLAYER = new Object();
    private static final List<Entry> entries = new ArrayList<>();
    private static final List<Npc> standing = new ArrayList<>();
    private static final Comparator<Entry> BY_DEPTH = Comparator.comparingDouble(Entry::depth);

    private DepthRenderer() {
    }

    static void draw() {
        for (Chunk chunk : WorldManager.activeChunks) {
            addWalls(chunk.walls);
            addWalls(chunk.halfWalls);
        }
        for (Npc npc : WorldManager.npcs) {
            if (npc.isActive() && isVisible(npc.x, npc.y)) {
                entries.add(new Entry(feet(npc), npc));
                standing.add(npc);
            }
        }
        float playerDepth = Player.y + Player.SIZE / 2;
        entries.add(new Entry(playerDepth, PLAYER));
        entries.sort(BY_DEPTH); // estable: a igual profundidad se mantiene el orden de arriba

        for (Entry entry : entries) {
            switch (entry.thing()) {
                case Struct s -> {
                    Draw.tint(1, 1, 1, wallAlpha(s, playerDepth));
                    WorldRenderer.drawWall(s);
                    Draw.tint(1, 1, 1, 1);
                }
                case Npc npc -> CharacterRenderer.drawNpc(npc);
                default -> CharacterRenderer.drawPlayer();
            }
        }
        entries.clear();
        standing.clear();
    }

    private static void addWalls(List<Struct> walls) {
        for (Struct s : walls) {
            if (isVisible(s.x(), s.y())) {
                entries.add(new Entry(WorldRenderer.depth(s), s));
            }
        }
    }

    private static float feet(Npc npc) {
        return npc.y + Npc.BODY_HALF;
    }

    /** Solo se aclaran los muros que están delante (más al sur) de quien taparían. */
    private static float wallAlpha(Struct s, float playerDepth) {
        float depth = WorldRenderer.depth(s);
        for (Npc npc : standing) {
            if (feet(npc) < depth && distanceToSprite(s, npc.x, npc.y) == 0) {
                return SEE_THROUGH_ALPHA;
            }
        }
        return playerDepth < depth ? fadeAroundPlayer(distanceToSprite(s, Player.x, Player.y)) : 1;
    }

    /** Opacidad de un muro delante del jugador según lo lejos que esté de él. */
    static float fadeAroundPlayer(float distance) {
        float t = Math.clamp((distance - FADE_NEAR) / (FADE_FAR - FADE_NEAR), 0f, 1f);
        return SEE_THROUGH_ALPHA + (1 - SEE_THROUGH_ALPHA) * t;
    }

    /**
     * Distancia entre la parte visible de un personaje en (x, y) y el sprite del muro (de su remate al pie de la
     * base); 0 si se solapan, es decir, si el muro lo taparía estando delante.
     */
    static float distanceToSprite(Struct s, float x, float y) {
        float dx = Math.max(0, Math.abs(x - s.x()) - s.halfWidth() - VISIBLE_HALF);
        float above = WorldRenderer.spriteTop(s) - (y + VISIBLE_HALF);
        float below = (y - VISIBLE_HALF) - WorldRenderer.depth(s);
        float dy = Math.max(0, Math.max(above, below));
        return (float) Math.hypot(dx, dy);
    }
}
