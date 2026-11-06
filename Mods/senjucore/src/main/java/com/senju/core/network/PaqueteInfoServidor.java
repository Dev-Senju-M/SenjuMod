package com.senju.core.network;

import java.util.function.Supplier;
import com.senju.core.cliente.DatosCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PaqueteInfoServidor(String nombreServidor, long limiteSegundos, boolean sinLimite) {
    public static void encode(PaqueteInfoServidor p, FriendlyByteBuf buf) {
        buf.writeUtf(p.nombreServidor);
        buf.writeLong(p.limiteSegundos);
        buf.writeBoolean(p.sinLimite);
    }

    public static PaqueteInfoServidor decode(FriendlyByteBuf buf) {
        return new PaqueteInfoServidor(buf.readUtf(), buf.readLong(), buf.readBoolean());
    }

    public static void handle(PaqueteInfoServidor p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DatosCliente.setInfo(p.nombreServidor, p.limiteSegundos, p.sinLimite)));
        c.setPacketHandled(true);
    }
}
