package graphics;

import static graphics.Camera.centerX;
import static graphics.Camera.centerY;
import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;
import static util.Geometry.dirX;
import static util.Geometry.dirY;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import items.Consumable;
import items.Gun;
import items.StructItem;
import npc.Npc;
import npc.NpcKind;
import player.Player;
import util.GameClock;
import world.WorldManager;

/** NPC (cadáveres, levantándose y en pie), el jugador con lo que lleva en la mano, sus sombras y barras de vida. */
final class CharacterRenderer {
    /** Tamaño en pantalla del sprite del jugador (también lo usan los NPC humanos). */
    private static final float CHARACTER_SIZE = 100;
    /**
     * Tamaño en pantalla de zombis y cadáveres. El jugador solo ocupa ~44% de su imagen (28 de 64 px) y los
     * zombis ~72% (22-24 de 32 px), así que con 62 px se ven del mismo tamaño que el jugador a 100 px (~44 px).
     */
    private static final float ZOMBIE_SIZE = 62;
    /** Charco bajo un humano abatido mientras se convierte en zombi. */
    private static final float BLOOD_PUDDLE_SIZE = 110;
    private static final float SHADOW_OFFSET = 4;
    /** Radio de la sombra de jugador y NPC; el cuerpo visible mide ~22 px de radio. */
    private static final float SHADOW_RADIUS = 16;
    private static final int SHADOW_SEGMENTS = 16;
    /** Duración del parpadeo al recibir un balazo. */
    private static final long HIT_FLASH_MS = 100;
    private static final float HEALTH_BAR_WIDTH = 50;
    private static final float HEALTH_BAR_HEIGHT = 5;

    private CharacterRenderer() {
    }

    /**
     * Lo que está tumbado en el suelo, bajo todo lo que tiene altura: cadáveres, charcos y cuerpos de los que se
     * están levantando, y las sombras de los NPC en pie y del jugador. Los cuerpos en pie los dibuja
     * {@link DepthRenderer} (con {@link #drawNpc} y {@link #drawPlayer}).
     */
    static void drawGround() {
        float puddleHalf = BLOOD_PUDDLE_SIZE / 2;
        for (Npc npc : WorldManager.npcs) {
            if (!isVisible(npc.x, npc.y)) {
                continue;
            }
            if (!npc.isAlive()) {
                drawNpc(npc); // cadáver: sprite de sangre
            } else if (npc.isRising()) {
                // Giro fijo por NPC para que no todos los charcos sean iguales
                float rotation = Math.floorMod(System.identityHashCode(npc), 360);
                Draw.sprite(TextureId.BLOOD, screenX(npc.x) - puddleHalf, screenY(npc.y) - puddleHalf,
                        BLOOD_PUDDLE_SIZE, BLOOD_PUDDLE_SIZE, rotation);
            } else {
                drawShadow(screenX(npc.x), screenY(npc.y));
            }
        }
        drawShadow(centerX(), centerY());

        // Abatidos que se están levantando, encima de los charcos
        for (Npc npc : WorldManager.npcs) {
            if (npc.isRising() && isVisible(npc.x, npc.y)) {
                drawNpc(npc);
            }
        }
    }

    /** Barras de vida de los NPC heridos y del jugador, por encima de todo el mundo. */
    static void drawHealthBars() {
        for (Npc npc : WorldManager.npcs) {
            if (npc.hp > 0 && npc.hp < 100 && isVisible(npc.x, npc.y)) {
                drawHealthBar(npc.hp, screenX(npc.x) - HEALTH_BAR_WIDTH / 2,
                        screenY(npc.y) - spriteSize(npc) / 2 + 5);
            }
        }
        if (Player.hp > 0) {
            drawHealthBar(Player.hp, centerX() - Player.SIZE / 2, centerY() - Player.SIZE - 5);
        }
    }

    /** Sprite del NPC (con tinte según su tipo y parpadeo si acaba de recibir un balazo). */
    static void drawNpc(Npc npc) {
        if (npc.kind == NpcKind.HUMAN) {
            // Tinte según el tipo para distinguir aliados, civiles y hostiles
            switch (npc.type) {
                case GUARD -> Draw.tint(0.6f, 1f, 0.6f, 1);
                case CIVILIAN -> Draw.tint(0.7f, 0.8f, 1f, 1);
                case SOLDIER -> Draw.tint(1f, 0.55f, 0.55f, 1);
                default -> { }
            }
        }
        // El sprite humano mira hacia abajo (sur) y el de los zombis hacia arriba (norte)
        boolean humanSprite = npc.texture == TextureId.HUMAN;
        float rotation = humanSprite ? npc.rotation - 90 : npc.rotation + 90;
        float size = spriteSize(npc);
        float half = size / 2;
        float time = animationTime(npc);
        Draw.animated(npc.texture, time, screenX(npc.x) - half, screenY(npc.y) - half, size, size, rotation);
        Draw.tint(1, 1, 1, 1);
        if (npc.wasHitWithin(HIT_FLASH_MS)) {
            // Parpadeo al recibir un balazo: se vuelve a dibujar el sprite con mezcla aditiva para aclararlo
            Draw.setAdditive(true);
            Draw.tint(1f, 0.75f, 0.75f, 0.85f);
            Draw.animated(npc.texture, time, screenX(npc.x) - half, screenY(npc.y) - half, size, size, rotation);
            Draw.setAdditive(false);
            Draw.tint(1, 1, 1, 1);
        }
    }

    /** Reloj de animación del NPC: el del juego con un desfase fijo por NPC, para que no vayan todos a la vez. */
    private static float animationTime(Npc npc) {
        return GameClock.now() + Math.floorMod(System.identityHashCode(npc), 10_000);
    }

    private static float spriteSize(Npc npc) {
        return npc.texture == TextureId.HUMAN ? CHARACTER_SIZE : ZOMBIE_SIZE;
    }

    /** El jugador con lo que lleva en la mano (su sombra va en {@link #drawGround}). */
    static void drawPlayer() {
        float cx = centerX();
        float cy = centerY();

        // El sprite del jugador mira hacia abajo (sur)
        float half = CHARACTER_SIZE / 2;
        Draw.animated(TextureId.PLAYER, GameClock.now(), cx - half, cy - half, CHARACTER_SIZE, CHARACTER_SIZE,
                Player.rotation - 90);

        // Objeto en la mano
        float handX = handX();
        float handY = handY();
        switch (Player.selectedItem()) {
            case null -> { }
            // 32 px: antes se dibujaba a 50 pero la imagen solo ocupaba 20/32 del cuadro (relleno)
            case Gun gun -> Draw.sprite(gun.heldTexture, handX - 16, handY - 16, 32, 32, Player.rotation);
            case Consumable consumable -> Draw.sprite(consumable.heldTexture,
                    handX - 12, handY - 12, 24, 24, Player.rotation);
            case StructItem struct -> Draw.sprite(struct.heldTexture,
                    handX - 20, handY - 20, 40, 40, Player.rotation);
        }
    }

    /** Posición en pantalla de la mano del jugador (donde se dibuja el arma y su fogonazo). */
    static float handX() {
        return centerX() + dirX(Player.rotation) * Player.HAND_OFFSET;
    }

    static float handY() {
        return centerY() + dirY(Player.rotation) * Player.HAND_OFFSET;
    }

    /** Sombra elíptica bajo un personaje, centrada en (x, y) de pantalla. */
    private static void drawShadow(float x, float y) {
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0, 0, 0, WorldRenderer.SHADOW_ALPHA);
        float width = SHADOW_RADIUS * 2, height = SHADOW_RADIUS * 2 * 0.8f;
        shapes.ellipse(x + SHADOW_OFFSET - width / 2, y + SHADOW_OFFSET - height / 2, width, height, SHADOW_SEGMENTS);
    }

    private static void drawHealthBar(int hp, float x, float y) {
        Draw.sprite(TextureId.HP_BACKGROUND, x, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        TextureId texture = hp > 50 ? TextureId.HP_HIGH : hp > 25 ? TextureId.HP_MEDIUM : TextureId.HP_LOW;
        Draw.sprite(texture, x, y, HEALTH_BAR_WIDTH * hp / 100f, HEALTH_BAR_HEIGHT);
    }
}
