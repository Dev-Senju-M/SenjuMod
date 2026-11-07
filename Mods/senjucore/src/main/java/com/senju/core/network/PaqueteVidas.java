package com.senju.core.network;

import java.util.function.Supplier;
import com.senju.core.cliente.DatosCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Vidas del propio jugador para el HUD (S2C). {@code vidas < 0}: sin vidas (staff exento). */
public record PaqueteVidas(int vidas, int total) {
    public static void encode(PaqueteVidas p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.vidas + 1);
        buf.writeVarInt(p.total);
    }

    public static PaqueteVidas decode(FriendlyByteBuf buf) {
        return new PaqueteVidas(buf.readVarInt() - 1, buf.readVarInt());
    }

    public static void handle(PaqueteVidas p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DatosCliente.setVidas(p.vidas, p.total)));
        c.setPacketHandled(true);
    }
}
