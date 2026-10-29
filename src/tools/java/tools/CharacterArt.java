package tools;

import java.awt.image.BufferedImage;

/**
 * Personajes provisionales en pixel art, vistos en 3/4 (estilo Necesse), para {@link PixelArtGenerator}.
 *
 * <p>Cada personaje mide 12x20 px (48x80 unidades del mundo) con los pies en la fila de abajo, y tiene:
 * <ul>
 *   <li>4 direcciones: {@code down}, {@code up}, {@code left} y {@code right} (izquierda = derecha en espejo).</li>
 *   <li>{@code idle}: quieto, una imagen. {@code walk}: andando, 4 frames ({@code _0} a {@code _3}).</li>
 *   <li>Los humanos, además, {@code lying}: tumbado (20x12), para cuando están en el suelo a punto de levantarse.</li>
 * </ul>
 * Se dibujan sin contorno y al final se les añade uno oscuro de 1 px.
 */
final class CharacterArt {
    static final int WIDTH = 12, HEIGHT = 20;
    static final int WALK_FRAMES = 4;
    static final String[] DIRECTIONS = {"down", "up", "left", "right"};

    private static final int EYE = 0x1e1e28, ZOMBIE_EYE = 0xd23a2a, SHOE = 0x2b2622;

    /** Colores de un personaje. {@code zombie}: brazos hacia delante, ojos rojos y ropa rota. */
    record Palette(int skin, int hair, int shirt, int pants, boolean zombie) {
    }

    static final Palette PLAYER = new Palette(0xe0b48c, 0x5a3a22, 0x3d6fb0, 0x3a3a48, false);
    /** Gris claro: el juego lo tinta según el tipo de NPC (guardia, civil o soldado). */
    static final Palette HUMAN = new Palette(0xe6c4a4, 0x4a4038, 0xd0d0d0, 0x8c8c8c, false);
    static final Palette ZOMBIE = new Palette(0x8fae7a, 0x3a3a30, 0x6b5a48, 0x4a4a5a, true);
    static final Palette ZOMBIE_ALT = new Palette(0xa9ad80, 0x5b4a3a, 0x7a3a3a, 0x3e4a3a, true);

    private CharacterArt() {
    }

    /**
     * Un frame. {@code frame}: -1 = quieto; 0 a 3 = ciclo de andar (0 y 2 con las piernas juntas, 1 y 3 dando
     * el paso con una pierna u otra).
     */
    static BufferedImage frame(Palette p, String direction, int frame) {
        Canvas c = new Canvas(WIDTH, HEIGHT);
        int step = frame == 1 ? 1 : frame == 3 ? -1 : 0;
        switch (direction) {
            case "down" -> front(c, p, step, false);
            case "up" -> front(c, p, step, true);
            case "right" -> side(c, p, step);
            case "left" -> {
                side(c, p, step);
                c.mirror();
            }
            default -> throw new IllegalArgumentException(direction);
        }
        return c.outlined();
    }

    /** Tumbado: el de perfil quieto, girado 90º (la cabeza a la derecha). */
    static BufferedImage lying(Palette p) {
        Canvas c = new Canvas(WIDTH, HEIGHT);
        side(c, p, 0);
        return c.rotated().outlined();
    }

    // ---------------------------------------------------------------------------------------------
    // De frente (o de espaldas)
    // ---------------------------------------------------------------------------------------------

    private static void front(Canvas c, Palette p, int step, boolean back) {
        int bob = step != 0 ? 1 : 0; // al dar el paso el cuerpo baja un píxel
        int head = 1 + bob, body = head + 7, legs = body + 6;

        // Cabeza (8 de ancho): pelo arriba y a los lados; de espaldas, todo pelo
        c.rect(2, head, 8, 7, p.skin());
        c.rect(2, head, 8, back ? 6 : 2, p.hair());
        if (!back) {
            c.set(2, head + 2, p.hair());
            c.set(9, head + 2, p.hair());
            int eye = p.zombie() ? ZOMBIE_EYE : EYE;
            c.set(4, head + 4, eye);
            c.set(7, head + 4, eye);
            if (p.zombie()) {
                c.rect(5, head + 6, 2, 1, Canvas.darker(p.skin(), 0.35f)); // boca abierta
            }
        }
        c.shadeRight(2, head, 8, 7);

        // Cuerpo (6 de ancho) con cinturón
        c.rect(3, body, 6, 6, p.shirt());
        c.rect(3, body + 5, 6, 1, Canvas.darker(p.pants(), 0.2f));
        if (p.zombie()) { // ropa rota
            c.set(4, body + 3, p.skin());
            c.set(7, body + 1, p.skin());
        }
        c.shadeRight(3, body, 6, 6);

        // Brazos: los humanos los balancean al andar; los zombis los llevan hacia delante
        if (p.zombie() && !back) {
            c.rect(2, body, 1, 2, p.shirt());
            c.rect(9, body, 1, 2, p.shirt());
            c.rect(2, body + 2, 2, 1, p.skin());
            c.rect(8, body + 2, 2, 1, p.skin());
        } else {
            arm(c, 2, body + Math.max(0, step), p);
            arm(c, 9, body + Math.max(0, -step), p);
        }

        // Piernas: al dar el paso, una se levanta un píxel
        int ground = HEIGHT - 2;
        c.rect(3, legs, 6, 1, p.pants());
        leg(c, 3, legs + 1, ground - (step > 0 ? 1 : 0), p);
        leg(c, 7, legs + 1, ground - (step < 0 ? 1 : 0), p);
    }

    /** Manga algo más oscura que la camiseta, para que el brazo se distinga del cuerpo, y la mano. */
    private static void arm(Canvas c, int x, int top, Palette p) {
        c.rect(x, top, 1, 3, Canvas.darker(p.shirt(), 0.3f));
        c.rect(x, top + 3, 1, 2, p.skin());
    }

    /** Pierna de 2 de ancho desde {@code top} hasta {@code bottom} (el zapato). */
    private static void leg(Canvas c, int x, int top, int bottom, Palette p) {
        c.rect(x, top, 2, bottom - top, p.pants());
        c.rect(x, bottom, 2, 1, SHOE);
    }

    // ---------------------------------------------------------------------------------------------
    // De perfil (mirando a la derecha)
    // ---------------------------------------------------------------------------------------------

    private static void side(Canvas c, Palette p, int step) {
        int bob = step != 0 ? 1 : 0;
        int head = 1 + bob, body = head + 7, legs = body + 6;

        // Cabeza (6 de ancho): pelo arriba y por detrás, un ojo delante
        c.rect(3, head, 6, 7, p.skin());
        c.rect(3, head, 6, 2, p.hair());
        c.rect(3, head + 2, 2, 3, p.hair());
        c.set(7, head + 4, p.zombie() ? ZOMBIE_EYE : EYE);

        // Cuerpo (4 de ancho)
        c.rect(4, body, 4, 6, p.shirt());
        c.rect(4, body + 5, 4, 1, Canvas.darker(p.pants(), 0.2f));
        if (p.zombie()) {
            c.set(5, body + 3, p.skin());
        }

        // Piernas: juntas, o una adelantada y otra atrasada
        int ground = HEIGHT - 2;
        c.rect(4, legs, 4, 1, p.pants());
        if (step == 0) {
            leg(c, 5, legs + 1, ground, p);
        } else {
            int front = step > 0 ? 1 : 0; // alterna qué pierna va delante (cambia el tono)
            int frontPants = front == 1 ? p.pants() : Canvas.darker(p.pants(), 0.25f);
            int backPants = front == 1 ? Canvas.darker(p.pants(), 0.25f) : p.pants();
            c.rect(3, legs + 1, 2, ground - legs - 1, backPants);
            c.rect(2, ground, 2, 1, SHOE);
            c.rect(6, legs + 1, 2, ground - legs - 1, frontPants);
            c.rect(7, ground, 2, 1, SHOE);
        }

        // Brazo: el humano lo balancea; el zombi lo lleva estirado hacia delante
        if (p.zombie()) {
            c.rect(6, body + 1, 3, 1, p.shirt());
            c.rect(9, body + 1, 2, 1, p.skin());
        } else {
            int swing = -step; // el brazo va al revés que la pierna
            c.rect(5 + swing, body, 2, 3, Canvas.darker(p.shirt(), 0.15f));
            c.rect(5 + swing * 2, body + 3, 2, 1, p.skin());
        }
    }
}
