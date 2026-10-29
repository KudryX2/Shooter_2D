package graphics;

import static graphics.Camera.centerX;
import static graphics.Camera.centerY;
import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;
import static util.Geometry.dirX;
import static util.Geometry.dirY;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import items.Consumable;
import items.Gun;
import items.Projectile;
import items.StructItem;
import npc.Npc;
import npc.NpcKind;
import player.Player;
import util.WalkCycle;
import world.WorldManager;

/**
 * NPC (cadáveres, levantándose y en pie), el jugador con lo que lleva en la mano, sus sombras y barras de vida.
 *
 * <p>Los personajes se ven en 3/4: no se giran, sino que se elige su imagen según hacia dónde miran ({@link Facing})
 * y si están andando. El sprite se apoya con los pies en su posición y "sube" en pantalla, así que la posición
 * (x, y) de un personaje queda a la altura de sus pies.
 */
final class CharacterRenderer {
    /** Tamaño del sprite de un personaje (12x20 px de pixel art, a 4 unidades por píxel). */
    static final float SPRITE_WIDTH = 48;
    static final float SPRITE_HEIGHT = 80;
    /** El borde de abajo del sprite (la suela) queda esto por debajo de la posición del personaje. */
    static final float FEET_BELOW = 8;
    /** Medio ancho de la parte pintada del sprite (cuerpo y brazos, sin el aire de los lados). */
    static final float BODY_HALF_WIDTH = 20;
    /** Altura de las manos sobre la posición del personaje: donde lleva el arma y por donde salen las balas. */
    private static final float HAND_HEIGHT = Projectile.FLIGHT_HEIGHT;
    /** Tumbado (20x12 px). */
    private static final float LYING_WIDTH = 80;
    private static final float LYING_HEIGHT = 48;
    private static final float CORPSE_SIZE = 62;
    /** Charco bajo un humano abatido mientras se convierte en zombi. */
    private static final float BLOOD_PUDDLE_SIZE = 110;
    private static final float SHADOW_WIDTH = 34;
    private static final float SHADOW_HEIGHT = 12;
    private static final int SHADOW_SEGMENTS = 16;
    /** Duración del parpadeo al recibir un balazo. */
    private static final long HIT_FLASH_MS = 100;
    /** 12x1 píxeles del pixel art (medidas que no sean múltiplo de un píxel salen irregulares). */
    private static final float HEALTH_BAR_WIDTH = 48;
    private static final float HEALTH_BAR_HEIGHT = 4;

    private CharacterRenderer() {
    }

    /** Lo alto del sprite (en coordenadas del mundo) de un personaje en {@code y}. */
    static float spriteTop(float y) {
        return y + FEET_BELOW - SPRITE_HEIGHT;
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
                drawCorpse(npc);
            } else if (npc.isRising()) {
                Draw.sprite(TextureId.BLOOD, screenX(npc.x) - puddleHalf, screenY(npc.y) - puddleHalf,
                        BLOOD_PUDDLE_SIZE, BLOOD_PUDDLE_SIZE, fixedRotation(npc));
            } else {
                drawShadow(screenX(npc.x), screenY(npc.y));
            }
        }
        drawShadow(centerX(), centerY());

        // Abatidos que se están levantando, tumbados encima de los charcos
        for (Npc npc : WorldManager.npcs) {
            if (npc.isRising() && isVisible(npc.x, npc.y)) {
                drawLying(npc);
            }
        }
    }

    /** Barras de vida de los NPC heridos y del jugador, sobre la cabeza y por encima de todo el mundo. */
    static void drawHealthBars() {
        for (Npc npc : WorldManager.npcs) {
            if (npc.hp > 0 && npc.hp < 100 && isVisible(npc.x, npc.y)) {
                drawHealthBar(npc.hp, screenX(npc.x), screenY(spriteTop(npc.y)));
            }
        }
        if (Player.hp > 0) {
            drawHealthBar(Player.hp, centerX(), screenY(spriteTop(Player.y)));
        }
    }

    /** NPC en pie con su arma, si lleva (tinte según su tipo y parpadeo si acaba de recibir un balazo). */
    static void drawNpc(Npc npc) {
        Gun gun = npc.inventory.firstGun(); // la misma con la que dispara
        boolean gunBehind = gun != null && isBehindBody(npc.rotation);
        if (gunBehind) {
            drawNpcGun(npc, gun);
        }

        TextureRegion frame = standing(npc.texture, npc.rotation, npc.walkCycle);
        float x = screenX(npc.x) - SPRITE_WIDTH / 2;
        float y = screenY(spriteTop(npc.y));
        tint(npc);
        Draw.region(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, 0);
        Draw.tint(1, 1, 1, 1);
        if (npc.wasHitWithin(HIT_FLASH_MS)) {
            flash(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT);
        }

        if (gun != null && !gunBehind) {
            drawNpcGun(npc, gun);
        }
    }

    private static void drawNpcGun(Npc npc, Gun gun) {
        float muzzleX = screenX(npc.x) + dirX(npc.rotation) * Npc.GUN_OFFSET;
        float muzzleY = screenY(npc.y) - HAND_HEIGHT + dirY(npc.rotation) * Npc.GUN_OFFSET;
        drawGun(gun, muzzleX, muzzleY, npc.rotation);
    }

    /** De espaldas (mirando hacia arriba) lo que lleva en la mano queda detrás del cuerpo. */
    private static boolean isBehindBody(float rotation) {
        return Facing.of(rotation) == Facing.UP;
    }

    /**
     * Objeto en la mano centrado en (handX, handY) de pantalla, girado hacia donde apunta. Si apunta a la izquierda
     * se voltea para que no quede boca abajo.
     */
    private static void drawHeld(TextureId texture, float handX, float handY, float size, float rotation) {
        drawHeld(texture, handX, handY, size, size, rotation);
    }

    private static void drawHeld(TextureId texture, float handX, float handY, float width, float height,
                                 float rotation) {
        Draw.sprite(texture, handX - width / 2, handY - height / 2, width, height, rotation, pointsLeft(rotation));
    }

    /**
     * Arma a su tamaño real en píxeles, con la boca del cañón en (muzzleX, muzzleY) de pantalla: ahí salen las balas y
     * el fogonazo, sea cual sea el arma. Su imagen tiene la empuñadura en el centro y el cañón (el lado largo) hacia
     * la derecha, así que una pistola queda junto al cuerpo y un fusil, delante del pecho.
     */
    private static void drawGun(Gun gun, float muzzleX, float muzzleY, float rotation) {
        TextureRegion region = TexturesManager.region(gun.heldTexture);
        int width = region.getRegionWidth();
        float art = DisplayManager.ART_PIXEL;
        float reach = ((width - 1) / 2 - 1) * art; // de la empuñadura a la boca (sin el contorno)
        float gripX = muzzleX - dirX(rotation) * reach;
        float gripY = muzzleY - dirY(rotation) * reach;
        drawHeld(gun.heldTexture, gripX, gripY, width * art, region.getRegionHeight() * art, rotation);
    }

    static boolean pointsLeft(float rotation) {
        return dirX(rotation) < 0;
    }

    private static TextureRegion standing(TextureId texture, float rotation, WalkCycle walk) {
        return TexturesManager.character(texture, Facing.of(rotation), walk.isWalking(), walk.distance());
    }

    /** Tinte según el tipo, para distinguir aliados, civiles y hostiles (todos usan el mismo sprite gris). */
    private static void tint(Npc npc) {
        if (npc.kind != NpcKind.HUMAN) {
            return;
        }
        switch (npc.type) {
            case GUARD -> Draw.tint(1f, 0.82f, 0.45f, 1); // ámbar: el verde se confundía con los zombis
            case CIVILIAN -> Draw.tint(0.7f, 0.8f, 1f, 1);
            case SOLDIER -> Draw.tint(1f, 0.55f, 0.55f, 1);
            default -> { }
        }
    }

    /** Parpadeo al recibir un balazo: se vuelve a dibujar el sprite con mezcla aditiva para aclararlo. */
    private static void flash(TextureRegion frame, float x, float y, float width, float height) {
        Draw.setAdditive(true);
        Draw.tint(1f, 0.75f, 0.75f, 0.85f);
        Draw.region(frame, x, y, width, height, 0);
        Draw.setAdditive(false);
        Draw.tint(1, 1, 1, 1);
    }

    /** Humano abatido, tumbado mientras se convierte en zombi. */
    private static void drawLying(Npc npc) {
        TextureRegion lying = TexturesManager.lying(npc.texture);
        if (lying == null) {
            drawNpc(npc);
            return;
        }
        float x = screenX(npc.x) - LYING_WIDTH / 2;
        float y = screenY(npc.y) - LYING_HEIGHT / 2;
        tint(npc);
        Draw.region(lying, x, y, LYING_WIDTH, LYING_HEIGHT, 0);
        Draw.tint(1, 1, 1, 1);
        if (npc.wasHitWithin(HIT_FLASH_MS)) {
            flash(lying, x, y, LYING_WIDTH, LYING_HEIGHT);
        }
    }

    /** Cadáver: un charco de sangre (su textura ya es {@link TextureId#BLOOD}). */
    private static void drawCorpse(Npc npc) {
        float half = CORPSE_SIZE / 2;
        Draw.sprite(npc.texture, screenX(npc.x) - half, screenY(npc.y) - half, CORPSE_SIZE, CORPSE_SIZE,
                fixedRotation(npc));
    }

    /** Giro fijo por NPC, para que no todos los charcos sean iguales. */
    private static float fixedRotation(Npc npc) {
        return Math.floorMod(System.identityHashCode(npc), 360);
    }

    /** El jugador con lo que lleva en la mano (su sombra va en {@link #drawGround}). */
    static void drawPlayer() {
        boolean heldBehind = isBehindBody(Player.rotation);
        if (heldBehind) {
            drawPlayerHeld();
        }
        TextureRegion frame = standing(TextureId.PLAYER, Player.rotation, Player.walkCycle);
        Draw.region(frame, centerX() - SPRITE_WIDTH / 2, screenY(spriteTop(Player.y)), SPRITE_WIDTH, SPRITE_HEIGHT, 0);
        if (!heldBehind) {
            drawPlayerHeld();
        }
    }

    private static void drawPlayerHeld() {
        float rotation = Player.rotation;
        switch (Player.selectedItem()) {
            case null -> { }
            case Gun gun -> drawGun(gun, handX(), handY(), rotation);
            case Consumable consumable -> drawHeld(consumable.heldTexture, handX(), handY(), 24, rotation);
            case StructItem struct -> drawHeld(struct.heldTexture, handX(), handY(), 40, rotation);
        }
    }

    /**
     * Posición en pantalla de la boca del arma del jugador, a la altura del pecho: de ahí salen las balas y el
     * fogonazo, y ahí se sujetan los objetos que no son armas.
     */
    static float handX() {
        return centerX() + dirX(Player.rotation) * Player.HAND_OFFSET;
    }

    static float handY() {
        return centerY() - HAND_HEIGHT + dirY(Player.rotation) * Player.HAND_OFFSET;
    }

    /** Sombra elíptica y aplastada (vista en 3/4) bajo los pies de un personaje en (x, y) de pantalla. */
    private static void drawShadow(float x, float y) {
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0, 0, 0, WorldRenderer.SHADOW_ALPHA);
        float feetY = y + FEET_BELOW - SHADOW_HEIGHT / 2;
        shapes.ellipse(x - SHADOW_WIDTH / 2, feetY - SHADOW_HEIGHT / 2, SHADOW_WIDTH, SHADOW_HEIGHT, SHADOW_SEGMENTS);
    }

    /** Barra centrada en {@code x}, justo encima de {@code top} (lo alto de la cabeza). */
    private static void drawHealthBar(int hp, float x, float top) {
        float left = x - HEALTH_BAR_WIDTH / 2;
        float y = top - HEALTH_BAR_HEIGHT - 4;
        Draw.sprite(TextureId.HP_BACKGROUND, left, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        TextureId texture = hp > 50 ? TextureId.HP_HIGH : hp > 25 ? TextureId.HP_MEDIUM : TextureId.HP_LOW;
        Draw.sprite(texture, left, y, HEALTH_BAR_WIDTH * hp / 100f, HEALTH_BAR_HEIGHT);
    }
}
