package com.senju.core.network;

import java.util.function.Supplier;
import com.senju.core.cliente.DatosCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PaqueteTiempo(long restantes, long usadosHoy) {
    public static void encode(PaqueteTiempo p, FriendlyByteBuf buf) {
        buf.writeLong(p.restantes);
        buf.writeLong(p.usadosHoy);
    }

    public static PaqueteTiempo decode(FriendlyByteBuf buf) {
        return new PaqueteTiempo(buf.readLong(), buf.readLong());
    }

    public static void handle(PaqueteTiempo p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DatosCliente.setTiempo(p.restantes, p.usadosHoy)));
        c.setPacketHandled(true);
    }
}
