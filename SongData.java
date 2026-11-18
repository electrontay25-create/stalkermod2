package com.stalkermod.entity;

import java.util.Arrays;

/**
 * Datos de la cancion de Anderdingus (analizados del audio): sirven para que su velocidad siga el ritmo.
 *  - CURVE : "intensidad" de 0 a 1 por segundo (sube con la cancion y nunca baja: cada vez va mas rapido).
 *  - ONSETS: segundos exactos de cada golpe de ritmo (tiempos desde que empieza a sonar la cancion).
 */
public final class SongData {

    /** Duracion de la cancion (s). Cuando termina, Anderdingus desaparece. */
    public static final float LENGTH_SECONDS = 56.0F;

    private static final float[] CURVE = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.025F, 0.046F, 0.064F, 0.097F, 0.159F, 0.252F, 0.349F, 0.415F, 0.436F, 0.436F, 0.436F, 0.436F, 0.436F, 0.436F, 0.436F, 0.436F, 0.436F, 0.436F, 0.48F, 0.549F, 0.634F, 0.724F, 0.803F, 0.861F, 0.894F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.901F, 0.909F, 0.948F, 0.978F, 0.997F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};

    private static final float[] ONSETS = {
            0.46F, 0.98F, 1.5F, 1.86F, 2.01F, 2.52F, 3.03F, 4.05F, 4.56F, 5.03F,
            6.11F, 6.62F, 7.64F, 8.15F, 8.66F, 9.18F, 9.69F, 9.95F, 10.46F, 11.74F,
            12.26F, 12.52F, 12.77F, 13.29F, 13.8F, 14.06F, 14.32F, 14.57F, 14.83F, 15.08F,
            15.34F, 15.59F, 15.85F, 16.1F, 16.36F, 16.87F, 17.12F, 17.38F, 17.9F, 18.41F,
            18.67F, 18.92F, 19.18F, 19.45F, 19.95F, 20.21F, 20.46F, 20.97F, 21.23F, 21.49F,
            21.65F, 22.01F, 22.27F, 22.52F, 22.77F, 23.03F, 23.16F, 23.8F, 24.04F, 24.73F,
            24.86F, 25.08F, 25.59F, 25.8F, 26.11F, 26.62F, 26.74F, 27.13F, 27.26F, 27.64F,
            27.77F, 27.97F, 28.15F, 28.42F, 28.67F, 29.18F, 29.44F, 29.7F, 29.9F, 30.02F,
            30.35F, 30.48F, 30.72F, 30.85F, 30.98F, 31.23F, 31.36F, 31.49F, 31.74F, 31.87F,
            31.99F, 32.23F, 32.48F, 32.73F, 32.94F, 33.23F, 33.48F, 33.73F, 33.87F, 33.99F,
            34.21F, 34.34F, 34.46F, 34.71F, 34.86F, 35.19F, 35.31F, 35.43F, 35.68F, 35.79F,
            35.92F, 36.14F, 36.4F, 36.63F, 36.87F, 37.09F, 37.35F, 37.57F, 37.77F, 38.28F,
            38.55F, 38.73F, 38.96F, 39.2F, 39.42F, 39.54F, 39.89F, 40.21F, 40.33F, 40.53F,
            40.77F, 40.99F, 41.22F, 41.33F, 41.64F, 41.88F, 42.09F, 42.21F, 42.34F, 42.74F,
            43.15F, 43.58F, 43.79F, 44.01F, 44.2F, 44.41F, 44.55F, 44.76F, 45.02F, 45.24F,
            45.43F, 45.64F, 45.93F, 46.22F, 46.43F, 46.61F, 46.82F, 47.01F, 47.19F, 47.45F,
            47.59F, 47.78F, 48.02F, 48.15F, 48.33F, 48.51F, 48.72F, 48.88F, 49.08F, 49.26F,
            49.45F, 49.8F, 49.96F, 50.1F, 50.31F, 50.49F, 50.62F, 50.77F, 51.0F, 51.18F,
            51.43F, 51.58F, 51.84F, 52.01F, 52.16F, 52.33F, 52.49F, 52.65F, 52.79F, 52.99F,
            53.12F, 53.29F, 53.45F, 53.57F, 53.79F, 54.04F, 54.25F, 54.54F, 54.75F, 54.9F,
            55.04F, 55.19F, 55.32F, 55.46F, 55.6F, 55.74F
    };

    private SongData() {}

    /** Intensidad 0..1 en el segundo t de la cancion (interpolada). */
    public static float curve(float t) {
        if (t <= 0.0F) {
            return CURVE[0];
        }
        if (t >= CURVE.length - 1) {
            return CURVE[CURVE.length - 1];
        }
        int i = (int) t;
        float f = t - i;
        return CURVE[i] * (1.0F - f) + CURVE[i + 1] * f;
    }

    /** Pulso 0..1: vale 1 justo en cada golpe del ritmo y decae rapido (~0.1 s). */
    public static float pulse(float t) {
        int idx = Arrays.binarySearch(ONSETS, t);
        if (idx < 0) {
            idx = -idx - 2;     // ultimo golpe anterior a t
        }
        if (idx < 0) {
            return 0.0F;
        }
        float dt = t - ONSETS[idx];
        return dt > 0.5F ? 0.0F : (float) Math.exp(-dt / 0.09F);
    }
}
