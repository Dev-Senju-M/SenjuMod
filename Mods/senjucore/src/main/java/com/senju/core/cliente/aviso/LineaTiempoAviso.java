package com.senju.core.cliente.aviso;

/**
 * Curvas del aviso dracónico (lógica pura, sin Minecraft). {@code t} = ms desde el inicio del aviso.
 * Todas las curvas devuelven [0,1] y van multiplicadas por el fundido final (5000–5600 ms),
 * salvo {@link #espaciadoTitulo(long)} que devuelve píxeles extra entre letras.
 */
public final class LineaTiempoAviso {
    public static final long DURACION = 5600;
    private static final long INICIO_FUNDIDO = 5000;

    private LineaTiempoAviso() {}

    /** 0 antes de {@code a}, 1 desde {@code b}, lineal entre medias. */
    static float rampa(long t, long a, long b) {
        if (t <= a) return 0f;
        if (t >= b) return 1f;
        return (float) (t - a) / (float) (b - a);
    }

    /** 1 hasta 5000 ms, baja a 0 en 5600 ms. */
    public static float fundido(long t) {
        return 1f - rampa(t, INICIO_FUNDIDO, DURACION);
    }

    /** Velo oscuro: 0→1 en 0–400 ms. */
    public static float velo(long t) {
        return rampa(t, 0, 400) * fundido(t);
    }

    /** Emblema (anillo y ojo cerrado): 300–700 ms. */
    public static float emblema(long t) {
        return rampa(t, 300, 700) * fundido(t);
    }

    /** Apertura del ojo: 500–1300 ms con ease-out cúbico. */
    public static float aperturaOjo(long t) {
        float p = rampa(t, 500, 1300);
        float q = 1f - p;
        return (1f - q * q * q) * fundido(t);
    }

    /** Escala de las alas: 0 antes de 600 ms, de 0.3 a 1 en 600–1500 ms. */
    public static float alas(long t) {
        if (t < 600) return 0f;
        return (0.3f + 0.7f * rampa(t, 600, 1500)) * fundido(t);
    }

    /** Opacidad del nombre: 1200–1700 ms. */
    public static float titulo(long t) {
        return rampa(t, 1200, 1700) * fundido(t);
    }

    /** Espaciado extra entre letras del nombre (px): de 10 a 0 en 1200–2000 ms. */
    public static float espaciadoTitulo(long t) {
        return 10f * (1f - rampa(t, 1200, 2000));
    }

    /** Opacidad del subtítulo: 1600–2100 ms. */
    public static float subtitulo(long t) {
        return rampa(t, 1600, 2100) * fundido(t);
    }

    /** Opacidad de los huevos: 2000–2500 ms. */
    public static float huevos(long t) {
        return rampa(t, 2000, 2500) * fundido(t);
    }

    public static boolean terminado(long t) {
        return t >= DURACION;
    }
}
