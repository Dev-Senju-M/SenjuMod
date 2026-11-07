package com.senju.core.staff.castigos;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gancho de efectos de mundo de un tipo de castigo. Se registra con
 * {@link Castigos#registrarEfecto(TipoCastigo, EfectoCastigo)} (un efecto por tipo; el último registrado gana).
 * {@code online} es null si el jugador está desconectado.
 */
public interface EfectoCastigo {
    /**
     * Se llama ANTES de registrar nada. Devolver {@link Castigos.Resultado#OK} para continuar o cualquier otro
     * resultado (p. ej. SIN_CARCEL) para vetar: no se guarda, no se registra y no se avisa.
     */
    default Castigos.Resultado puedeAplicar(MinecraftServer server, UUID id, @Nullable ServerPlayer online,
            TipoCastigo tipo) {
        return Castigos.Resultado.OK;
    }

    /**
     * Se llama DESPUÉS de guardar estado, historial, log y avisos. Para tipos con duración el castigo ya está
     * activo ({@link Castigos#activo}); {@code datos.vence()} es el vencimiento. Para EXPULSION / QUITAR_HORAS
     * {@code datos.duracionMs()} es la duración pedida (QUITAR_HORAS: PERMANENTE = todo el día).
     */
    void alAplicar(MinecraftServer server, UUID id, @Nullable ServerPlayer online, Castigos.Aplicado datos);

    /**
     * Se llama al perdonar o al vencer, con el castigo ya quitado del estado. Si el jugador está desconectado
     * ({@code online} null) el efecto debe diferir lo necesario al próximo login.
     */
    default void alQuitar(MinecraftServer server, UUID id, @Nullable ServerPlayer online) {}
}
