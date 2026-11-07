package com.senju.core.cliente;

import com.senju.core.network.PaqueteAviso;
import java.util.ArrayDeque;
import java.util.Deque;

/** Estado del panel en el cliente. Solo cliente; no cargar desde el servidor dedicado. */
public final class DatosCliente {
    private static String nombreServidor = "";
    private static boolean sinLimite = false;
    private static long restantes = 0;
    private static long usadosHoy = 0;
    private static boolean tieneDatos = false;
    private static boolean vanish = false;
    private static int ticks = 0;
    private static int vidas = -1;
    private static int vidasTotal = 0;
    private static final Deque<PaqueteAviso> AVISOS = new ArrayDeque<>();

    private DatosCliente() {}

    public static String nombreServidor() { return nombreServidor; }
    public static boolean sinLimite() { return sinLimite; }
    public static long restantes() { return restantes; }
    public static long usadosHoy() { return usadosHoy; }
    public static boolean tieneDatos() { return tieneDatos; }
    public static boolean vanish() { return vanish; }

    /** Vidas propias; -1 = el servidor no las envía / exento. */
    public static int vidas() { return vidas; }
    public static int vidasTotal() { return vidasTotal; }
    public static void setVidas(int v, int total) { vidas = v; vidasTotal = total; }

    /** Cola de avisos dracónicos pendientes; la tarea del overlay la vacía (hilo de cliente). */
    public static Deque<PaqueteAviso> avisos() { return AVISOS; }
    public static void encolarAviso(PaqueteAviso a) {
        AVISOS.addLast(a);
        while (AVISOS.size() > 20) AVISOS.removeFirst();
    }

    public static void setVanish(boolean activo) { vanish = activo; }

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
        vanish = false;
        ticks = 0;
        vidas = -1;
        vidasTotal = 0;
        AVISOS.clear();
    }
}
