package com.senju.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Jugadores desconectados por el mod durante su login (límite diario, ban, eliminado). En 1.20.1
 * {@code ServerPlayer.hasDisconnected()} sigue en false hasta un tick posterior, así que los handlers de login
 * posteriores consultan este registro. Solo hilo del servidor; se limpia al salir el jugador y al parar el servidor.
 */
public final class ExpulsadosAlEntrar {
    private static final Set<UUID> SET = new HashSet<>();

    private ExpulsadosAlEntrar() {}

    public static void marcar(UUID id) { SET.add(id); }
    public static boolean esta(UUID id) { return SET.contains(id); }
    public static void quitar(UUID id) { SET.remove(id); }
    public static void limpiar() { SET.clear(); }
}
