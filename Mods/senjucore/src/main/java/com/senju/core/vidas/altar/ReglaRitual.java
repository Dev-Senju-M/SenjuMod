package com.senju.core.vidas.altar;

import java.util.ArrayList;
import java.util.List;

/** Reglas puras del ritual de resurrección. */
public final class ReglaRitual {
    private ReglaRitual() {}

    public enum Falta { ESTRUCTURA, NOCHE, JUGADORES, TOTEM, PERGAMINO, NO_ELIMINADO }

    public record Condiciones(boolean estructuraOk, boolean esDeNoche, boolean requiereNoche, int jugadoresCerca,
                              int jugadoresNecesarios, boolean tieneTotem, String nombrePergamino,
                              boolean objetivoEliminado) {}

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
        return f;
    }
}
