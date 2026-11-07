package com.senju.core.staff;

import java.util.List;
import java.util.Locale;

/** Regla pura para decidir si un jugador es staff. */
public final class ReglaStaff {
    private ReglaStaff() {}

    public static boolean esStaff(boolean esOperador, String nombre, List<? extends String> lista,
            boolean incluyeOperadores) {
        if (esOperador && incluyeOperadores) return true;
        if (nombre == null || lista == null) return false;
        String n = nombre.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) return false;
        for (String s : lista) {
            if (s == null) continue;
            String e = s.trim().toLowerCase(Locale.ROOT);
            if (!e.isEmpty() && e.equals(n)) return true;
        }
        return false;
    }
}
