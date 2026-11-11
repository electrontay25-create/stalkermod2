package com.stalkermod.entity;

/**
 * Medidas (en bloques) de cada sprite 2D del Stalker.
 *  width/height : tamano en el mundo
 *  anchor       : fraccion del ancho (desde la izquierda) donde esta el borde recto que se pega a la cobertura
 *  headFx/headFy: centro de la cabeza como fraccion del ancho / alto (desde arriba-izquierda)
 */
public final class SpriteInfo {

    public final float width;
    public final float height;
    public final float anchor;
    public final float headFx;
    public final float headFy;

    private SpriteInfo(float width, float height, float anchor, float headFx, float headFy) {
        this.width = width;
        this.height = height;
        this.anchor = anchor;
        this.headFx = headFx;
        this.headFy = headFy;
    }

    /** Forma A: figura completa inclinada, de lado, pegada al borde del arbol/pared. */
    public static final SpriteInfo A = new SpriteInfo(1.735F, 3.8F, 0.0F, 0.257F, 0.07F);
    /** Forma B: cabeza ladeada con un brazo levantado que se agarra de la cobertura. */
    public static final SpriteInfo B = new SpriteInfo(1.214F, 1.807F, 0.365F, 0.747F, 0.215F);
    /** Solo la cara (para la ventana). */
    public static final SpriteInfo HEAD = new SpriteInfo(0.62F, 0.73F, 0.0F, 0.5F, 0.5F);

    public static SpriteInfo get(int sprite) {
        switch (sprite) {
            case StalkerEntity.SPRITE_B:
                return B;
            case StalkerEntity.SPRITE_HEAD:
                return HEAD;
            default:
                return A;
        }
    }

    /** Altura (sobre los pies) del centro de la cabeza cuando el sprite esta de pie en el suelo. */
    public float headHeight() {
        return this.height * (1.0F - this.headFy);
    }

    /** Distancia lateral (bloques) del centro de la cabeza respecto al borde pegado a la cobertura. */
    public float headLateral() {
        return (this.headFx - this.anchor) * this.width;
    }
}
