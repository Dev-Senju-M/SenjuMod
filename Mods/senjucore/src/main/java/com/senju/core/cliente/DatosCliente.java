package com.senju.core.cliente;

/** Estado del panel en el cliente. Solo cliente; no cargar desde el servidor dedicado. */
public final class DatosCliente {
    private static String nombreServidor = "";
    private static boolean sinLimite = false;
    private static long restantes = 0;
    private static long usadosHoy = 0;
    private static boolean tieneDatos = false;
    private static int ticks = 0;

    private DatosCliente() {}

    public static String nombreServidor() { return nombreServidor; }
    public static boolean sinLimite() { return sinLimite; }
    public static long restantes() { return restantes; }
    public static long usadosHoy() { return usadosHoy; }
    public static boolean tieneDatos() { return tieneDatos; }

    public static void setInfo(String nombre, long limiteSegundos, boolean sinLim) {
        nombreServidor = nombre;
        sinLimite = sinLim;
        tieneDatos = true;
    }

    public static void setTiempo(long rest, long usados) {
        restantes = Math.max(0, rest);
        usadosHoy = Math.max(0, usados);
        ticks = 0;
        tieneDatos = true;
    }

    /** Llamar cada tick de cliente; cada 20 ticks avanza un segundo. */
    public static void tick() {
        if (++ticks < 20) return;
        ticks = 0;
        if (restantes > 0) restantes--;
        usadosHoy++;
    }

    public static void limpiar() {
        nombreServidor = "";
        sinLimite = false;
        restantes = 0;
        usadosHoy = 0;
        tieneDatos = false;
        ticks = 0;
    }
}
