package com.senju.core.tiempo;

import java.util.List;
import java.util.OptionalInt;

/** Reglas puras del límite diario de tiempo. */
public final class ReglaTiempo {
    private ReglaTiempo() {}

    public static EstadoTiempo normalizar(EstadoTiempo e, long hoy) {
        return hoy > e.dia() ? new EstadoTiempo(hoy, 0) : e;
    }

    public static EstadoTiempo sumar(EstadoTiempo e, long hoy, long segundos) {
        EstadoTiempo n = normalizar(e, hoy);
        return new EstadoTiempo(n.dia(), n.usados() + segundos);
    }

    public static long restantes(EstadoTiempo e, long limiteSeg) {
        return Math.max(0, limiteSeg - e.usados());
    }

    public static boolean agotado(EstadoTiempo e, long limiteSeg) {
        return restantes(e, limiteSeg) == 0;
    }

    public static EstadoTiempo dar(EstadoTiempo e, long minutos) {
        return new EstadoTiempo(e.dia(), Math.max(0, e.usados() - minutos * 60));
    }

    /** Resta tiempo del día: usados += minutos*60 sin pasar del límite; minutos < 0 = todo el día. */
    public static EstadoTiempo quitar(EstadoTiempo e, long minutos, long limiteSeg) {
        if (minutos < 0) return new EstadoTiempo(e.dia(), Math.max(e.usados(), limiteSeg));
        long nuevo = Math.min(limiteSeg, e.usados() + minutos * 60);
        return new EstadoTiempo(e.dia(), Math.max(e.usados(), nuevo));
    }

    /** Aviso m con restantesAntes > m*60 >= restantesDespues; si varios, el menor. */
    public static OptionalInt avisoCruzado(long restantesAntes, long restantesDespues, List<Integer> avisosMin) {
        return avisosMin.stream()
                .filter(m -> restantesAntes > m * 60L && m * 60L >= restantesDespues)
                .mapToInt(Integer::intValue)
                .min();
    }

    public static long limiteSegundos(double horas) {
        return Math.round(horas * 3600);
    }
}
