package com.senju.core.network;

import java.util.function.Supplier;
import com.senju.core.cliente.DatosCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PaqueteVanish(boolean activo) {
    public static void encode(PaqueteVanish p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.activo);
    }

    public static PaqueteVanish decode(FriendlyByteBuf buf) {
        return new PaqueteVanish(buf.readBoolean());
    }

    public static void handle(PaqueteVanish p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DatosCliente.setVanish(p.activo)));
        c.setPacketHandled(true);
    }
}
