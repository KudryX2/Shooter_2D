package graphics;

import static graphics.Camera.centerX;
import static graphics.Camera.centerY;
import static graphics.Camera.isVisible;
import static graphics.Camera.screenX;
import static graphics.Camera.screenY;
import static util.Geometry.dirX;
import static util.Geometry.dirY;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import assets.TextureId;
import items.Consumable;
import items.Gun;
import items.MeleeWeapon;
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
    /**
     * Altura de la boca del arma sobre la posición del personaje, por donde salen las balas. La empuñadura queda más
     * abajo, en las manos del sprite ({@link items.GunType#barrelRise}).
     */
    private static final float MUZZLE_HEIGHT = Projectile.FLIGHT_HEIGHT;
    /** Altura de las manos del sprite: ahí se sujetan los objetos que no son armas de fuego. */
    private static final float HAND_HEIGHT = 24;
    /** Distancia del centro del cuerpo a la mano que sujeta un arma cuerpo a cuerpo. */
    private static final float MELEE_HAND_OFFSET = 16;
    /** Cada cuántos grados va un tramo de la estela del golpe. */
    private static final float TRAIL_STEP_DEGREES = 8;
    /** Un píxel del pixel art. */
    private static final float TRAIL_WIDTH = 4;
    private static final Color trailNew = new Color(1, 1, 1, 0);
    private static final Color trailOld = new Color(1, 1, 1, 0);
    /** Tumbado (20x12 px). */
    private static final float LYING_WIDTH = 80;
    private static final float LYING_HEIGHT = 48;
    /** La mancha de sangre (16x16 px) a su tamaño real. */
    private static final float CORPSE_SIZE = 64;
    /** Charco bajo un humano abatido mientras se convierte en zombi. */
    private static final float BLOOD_PUDDLE_SIZE = 128;
    private static final float SHADOW_WIDTH = 34;
    private static final float SHADOW_HEIGHT = 12;
    private static final int SHADOW_SEGMENTS = 16;
    /** Aviso de un ataque cuerpo a cuerpo (un mordisco): se echa 2 px atrás y se lanza 3 px hacia delante. */
    private static final float ATTACK_PULL_BACK = 8;
    private static final float ATTACK_LUNGE = 12;
    private static final long ATTACK_LUNGE_MS = 120;
    /** Cuánto pierde de verde y azul (se vuelve rojo) justo antes de atacar. */
    private static final float ATTACK_RED = 0.65f;
    /** Duración del parpadeo al recibir un balazo. */
    private static final long HIT_FLASH_MS = 100;
    /** 12x2 píxeles del pixel art (medidas que no sean múltiplo de un píxel salen irregulares). */
    private static final float HEALTH_BAR_WIDTH = 48;
    private static final float HEALTH_BAR_HEIGHT = 8;

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
        float lean = attackLean(npc);
        float x = screenX(npc.x) - SPRITE_WIDTH / 2 + dirX(npc.rotation) * lean;
        float y = screenY(spriteTop(npc.y)) + dirY(npc.rotation) * lean;
        tint(npc);
        float charge = npc.attackCharge();
        if (charge > 0) { // se va poniendo rojo mientras prepara el ataque
            Draw.tint(1, 1 - ATTACK_RED * charge, 1 - ATTACK_RED * charge, 1);
        }
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
        float muzzleY = screenY(npc.y) - MUZZLE_HEIGHT + dirY(npc.rotation) * Npc.GUN_OFFSET;
        drawGun(gun, muzzleX, muzzleY, npc.rotation);
    }

    /**
     * Cuánto se desplaza el sprite hacia donde mira por su ataque cuerpo a cuerpo: se echa atrás mientras lo prepara
     * y se lanza hacia delante un instante al atacar. Solo el dibujo: su posición no cambia.
     */
    static float attackLean(Npc npc) {
        float lean = -ATTACK_PULL_BACK * npc.attackCharge();
        long since = npc.millisSinceAttack();
        if (since >= 0 && since < ATTACK_LUNGE_MS) {
            lean += ATTACK_LUNGE * (1 - since / (float) ATTACK_LUNGE_MS);
        }
        return lean;
    }

    /** De espaldas (mirando hacia arriba) lo que lleva en la mano queda detrás del cuerpo. */
    private static boolean isBehindBody(float rotation) {
        return Facing.of(rotation) == Facing.UP;
    }

    /**
     * Objeto en la mano centrado en (handX, handY) de pantalla, girado hacia donde apunta. Si apunta a la izquierda
     * se voltea para que no quede boca abajo.
     */
    private static void drawHeld(TextureId texture, float handX, float handY, float width, float height,
                                 float rotation) {
        Draw.sprite(texture, handX - width / 2, handY - height / 2, width, height, rotation, pointsLeft(rotation));
    }

    /**
     * Arma a su tamaño real en píxeles, con la boca del cañón en (muzzleX, muzzleY) de pantalla: ahí salen las balas y
     * el fogonazo, sea cual sea el arma. Su imagen tiene la empuñadura en el centro y el cañón (el lado largo) hacia
     * la derecha, así que una pistola queda junto al cuerpo y un fusil, delante del pecho. El cañón queda
     * {@link items.GunType#barrelRise} píxeles por encima de la empuñadura, así que la imagen se baja eso (hacia su
     * "abajo", que gira con ella) para que la boca quede en su sitio y la empuñadura, en las manos.
     */
    private static void drawGun(Gun gun, float muzzleX, float muzzleY, float rotation) {
        TextureRegion region = TexturesManager.region(gun.heldTexture);
        int width = region.getRegionWidth();
        float art = DisplayManager.ART_PIXEL;
        float reach = ((width - 1) / 2 - 1) * art; // de la empuñadura a la boca (sin el contorno)
        float rise = gun.type.barrelRise * art;
        // "Arriba" de la imagen una vez girada (y volteada si apunta a la izquierda), en pantalla con la y hacia abajo
        float side = pointsLeft(rotation) ? -1 : 1;
        float upX = side * dirY(rotation);
        float upY = -side * dirX(rotation);
        float gripX = muzzleX - dirX(rotation) * reach - upX * rise;
        float gripY = muzzleY - dirY(rotation) * reach - upY * rise;
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
            case MeleeWeapon melee -> drawMelee(melee, rotation);
            case Consumable consumable -> drawHeldAtPixelSize(consumable.heldTexture, 1, rotation);
            // Su icono ocupa una casilla entera: a la mitad
            case StructItem struct -> drawHeldAtPixelSize(struct.heldTexture, 0.5f, rotation);
        }
    }

    /**
     * Arma cuerpo a cuerpo en la mano del jugador. En reposo está levantada hacia un lado del arco del golpe
     * (hacia arriba en pantalla); al golpear barre el arco entero hacia el otro lado, dejando una estela.
     */
    private static void drawMelee(MeleeWeapon melee, float rotation) {
        float progress = melee.swingProgress();
        float side = pointsLeft(rotation) ? -1 : 1; // con el arma volteada el barrido va al revés
        float start = rotation - side * melee.type.arc / 2;
        float eased = 1 - (1 - progress) * (1 - progress); // rápido al principio y frenando al final
        float angle = progress >= 1 ? start : start + side * melee.type.arc * eased;

        TextureRegion region = TexturesManager.region(melee.heldTexture);
        float art = DisplayManager.ART_PIXEL;
        float gripX = centerX() + dirX(angle) * MELEE_HAND_OFFSET;
        float gripY = centerY() - HAND_HEIGHT + dirY(angle) * MELEE_HAND_OFFSET;
        if (progress < 1) {
            float tip = MELEE_HAND_OFFSET + ((region.getRegionWidth() - 1) / 2 - 1) * art;
            drawSwingTrail(start, angle, side, tip, 1 - progress);
        }
        float width = region.getRegionWidth() * art, height = region.getRegionHeight() * art;
        // Volteada según hacia dónde apunta, no según el ángulo del barrido: si no, se daría la vuelta a medio golpe
        Draw.sprite(melee.heldTexture, gripX - width / 2, gripY - height / 2, width, height, angle,
                pointsLeft(rotation));
    }

    /**
     * Estela del golpe: un arco a la distancia de la punta del arma, desde donde empezó el barrido hasta donde va,
     * más transparente cuanto más atrás y según se acaba ({@code fade}, de 1 a 0).
     */
    private static void drawSwingTrail(float start, float end, float side, float radius, float fade) {
        ShapeRenderer shapes = Draw.shapes();
        float sweep = Math.abs(end - start);
        float centerX = centerX(), centerY = centerY() - HAND_HEIGHT;
        for (float done = 0; done < sweep; done += TRAIL_STEP_DEGREES) {
            float next = Math.min(sweep, done + TRAIL_STEP_DEGREES);
            float a0 = start + side * done, a1 = start + side * next;
            trailOld.a = 0.6f * fade * done / sweep;
            trailNew.a = 0.6f * fade * next / sweep;
            shapes.rectLine(centerX + dirX(a0) * radius, centerY + dirY(a0) * radius,
                    centerX + dirX(a1) * radius, centerY + dirY(a1) * radius, TRAIL_WIDTH, trailOld, trailNew);
        }
    }

    /** Objeto que no es un arma, en la mano del jugador, a su tamaño en píxeles del dibujo por {@code scale}. */
    private static void drawHeldAtPixelSize(TextureId texture, float scale, float rotation) {
        TextureRegion region = TexturesManager.region(texture);
        float size = DisplayManager.ART_PIXEL * scale;
        drawHeld(texture, handX(), handY() + MUZZLE_HEIGHT - HAND_HEIGHT, region.getRegionWidth() * size,
                region.getRegionHeight() * size, rotation);
    }

    /**
     * Posición en pantalla de la boca del arma del jugador: de ahí salen las balas y el fogonazo. Los objetos que no
     * son armas se sujetan justo debajo, a la altura de las manos.
     */
    static float handX() {
        return centerX() + dirX(Player.rotation) * Player.HAND_OFFSET;
    }

    static float handY() {
        return centerY() - MUZZLE_HEIGHT + dirY(Player.rotation) * Player.HAND_OFFSET;
    }

    /** Sombra elíptica y aplastada (vista en 3/4) bajo los pies de un personaje en (x, y) de pantalla. */
    private static void drawShadow(float x, float y) {
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0, 0, 0, WorldRenderer.SHADOW_ALPHA);
        float feetY = y + FEET_BELOW - SHADOW_HEIGHT / 2;
        shapes.ellipse(x - SHADOW_WIDTH / 2, feetY - SHADOW_HEIGHT / 2, SHADOW_WIDTH, SHADOW_HEIGHT, SHADOW_SEGMENTS);
    }

    /**
     * Barra centrada en {@code x}, justo encima de {@code top} (lo alto de la cabeza), con un contorno oscuro de un
     * píxel. El relleno va por píxeles enteros del dibujo (con 1 de vida ya se ve uno).
     */
    private static void drawHealthBar(int hp, float x, float top) {
        float art = DisplayManager.ART_PIXEL;
        float left = x - HEALTH_BAR_WIDTH / 2;
        float y = top - HEALTH_BAR_HEIGHT - art * 2;
        ShapeRenderer shapes = Draw.shapes();
        shapes.setColor(0.09f, 0.08f, 0.07f, 1);
        shapes.rect(left - art, y - art, HEALTH_BAR_WIDTH + art * 2, HEALTH_BAR_HEIGHT + art * 2);
        Draw.sprite(TextureId.HP_BACKGROUND, left, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        TextureId texture = hp > 50 ? TextureId.HP_HIGH : hp > 25 ? TextureId.HP_MEDIUM : TextureId.HP_LOW;
        float pixels = (float) Math.ceil(HEALTH_BAR_WIDTH / art * Math.clamp(hp, 0, 100) / 100f);
        Draw.sprite(texture, left, y, pixels * art, HEALTH_BAR_HEIGHT);
    }
}
