package com.senju.core.vidas.altar;

import java.util.List;

/** Lógica pura de la guía del altar: estado de cada posición de la estructura frente a lo esperado. */
public final class EstadoGuia {
    private EstadoGuia() {}

    public enum Esperado { OBSIDIANA, FUEGO_ALMAS }

    /** FALTA = aire/reemplazable; INCORRECTO = otro bloque distinto. */
    public enum EstadoPos { OK, FALTA, INCORRECTO }

    /** Tipo lógico de {@link EstructuraAltar} que corresponde a lo esperado. */
    public static String tipoLogico(Esperado e) {
        return e == Esperado.OBSIDIANA ? EstructuraAltar.OBSIDIANA : EstructuraAltar.FUEGO_ALMAS;
    }

    public static EstadoPos estado(Esperado e, String tipoLogico) {
        String bueno = tipoLogico(e);
        if (bueno.equals(tipoLogico)) return EstadoPos.OK;
        if ("aire".equals(tipoLogico)) return EstadoPos.FALTA;
        return EstadoPos.INCORRECTO;
    }

    /** Estados en el orden de {@link EstructuraAltar#posiciones()} (3 por pilar). */
    public static int pilaresCompletos(List<EstadoPos> estados) {
        int n = 0;
        for (int p = 0; p + 2 < estados.size(); p += 3) {
            if (estados.get(p) == EstadoPos.OK && estados.get(p + 1) == EstadoPos.OK
                    && estados.get(p + 2) == EstadoPos.OK) n++;
        }
        return n;
    }

    /** {@code {obsidianas, antorchas}} que faltan (FALTA e INCORRECTO cuentan). */
    public static int[] faltan(List<Esperado> esperados, List<EstadoPos> estados) {
        int[] r = new int[2];
        for (int i = 0; i < estados.size(); i++) {
            if (estados.get(i) == EstadoPos.OK) continue;
            r[esperados.get(i) == Esperado.OBSIDIANA ? 0 : 1]++;
        }
        return r;
    }
}
