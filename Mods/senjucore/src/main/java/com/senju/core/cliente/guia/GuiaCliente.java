package com.senju.core.cliente.guia;

import java.util.List;
import com.senju.core.network.PaqueteGuia;
import net.minecraft.core.BlockPos;

/** Guía del altar recibida del servidor (solo almacén; sin imports de cliente). */
public final class GuiaCliente {
    private GuiaCliente() {}

    private static BlockPos altar;
    private static List<PaqueteGuia.Entrada> entradas = List.of();
    private static boolean activa;

    public static void recibir(BlockPos altarPos, List<PaqueteGuia.Entrada> nuevas, boolean esActiva) {
        if (!esActiva) {
            limpiar();
            return;
        }
        altar = altarPos;
        entradas = List.copyOf(nuevas);
        activa = true;
    }

    public static void limpiar() {
        altar = null;
        entradas = List.of();
        activa = false;
    }

    public static boolean activa() { return activa; }

    public static BlockPos altar() { return altar; }

    public static List<PaqueteGuia.Entrada> entradas() { return entradas; }
}
