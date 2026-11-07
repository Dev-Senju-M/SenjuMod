package com.senju.core.vidas.altar;

import java.util.ArrayList;
import java.util.List;

/** Reglas puras del ritual de resurrección. */
public final class ReglaRitual {
    private ReglaRitual() {}

    public enum Falta { ESTRUCTURA, NOCHE, JUGADORES, TOTEM, PERGAMINO, NO_ELIMINADO, YA_RESUCITANDO }

    public record Condiciones(boolean estructuraOk, boolean esDeNoche, boolean requiereNoche, int jugadoresCerca,
                              int jugadoresNecesarios, boolean tieneTotem, String nombrePergamino,
                              boolean objetivoEliminado, boolean revivirPendiente) {}

    /** Noche real: tiempo del dia (mod 24000) entre 13000 y 23000; la lluvia de dia no cuenta. */
    public static boolean esDeNoche(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 13000 && t <= 23000;
    }

    /** Lista de cosas que faltan, en orden fijo; vacía si el ritual puede hacerse. */
    public static List<Falta> verificar(Condiciones c) {
        List<Falta> f = new ArrayList<>();
        if (!c.estructuraOk()) f.add(Falta.ESTRUCTURA);
        if (c.requiereNoche() && !c.esDeNoche()) f.add(Falta.NOCHE);
        if (c.jugadoresCerca() < c.jugadoresNecesarios()) f.add(Falta.JUGADORES);
        if (!c.tieneTotem()) f.add(Falta.TOTEM);
        boolean sinNombre = c.nombrePergamino() == null || c.nombrePergamino().isBlank();
        if (sinNombre) f.add(Falta.PERGAMINO);
        else if (!c.objetivoEliminado()) f.add(Falta.NO_ELIMINADO);
        else if (c.revivirPendiente()) f.add(Falta.YA_RESUCITANDO);
        return f;
    }
}
