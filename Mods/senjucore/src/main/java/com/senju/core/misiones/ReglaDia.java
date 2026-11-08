package com.senju.core.misiones;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lógica pura del cambio de día de las misiones. */
public final class ReglaDia {
    private ReglaDia() {}

    public static boolean cambioDeDia(long diaGlobal, long hoy) {
        return hoy > diaGlobal;
    }

    /** Conocidos que ni completaron ni están exentos, en el orden de {@code conocidos}. */
    public static List<UUID> incumplidores(Collection<UUID> conocidos, Set<UUID> completaron, Set<UUID> exentos) {
        List<UUID> r = new ArrayList<>();
        for (UUID u : conocidos) {
            if (!completaron.contains(u) && !exentos.contains(u)) r.add(u);
        }
        return r;
    }

    public static long segundosHastaMedianoche(LocalDateTime ahora) {
        LocalDateTime medianoche = ahora.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(ahora, medianoche).getSeconds();
    }

    public static boolean tocaAviso(long segundosHastaMedianoche, int minutosAntes) {
        return segundosHastaMedianoche > 0 && segundosHastaMedianoche <= minutosAntes * 60L;
    }
}
