package com.senju.core.network;

import java.util.function.Supplier;
import com.senju.core.cliente.DatosCliente;
import com.senju.core.vidas.TipoAviso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Aviso dracónico (S2C): nombre, vidas restantes, total y tipo. Se encola en el cliente. */
public record PaqueteAviso(String nombre, int vidas, int total, TipoAviso tipo) {
    public static void encode(PaqueteAviso p, FriendlyByteBuf buf) {
        buf.writeUtf(p.nombre, 64);
        buf.writeVarInt(p.vidas);
        buf.writeVarInt(p.total);
        buf.writeEnum(p.tipo);
    }

    public static PaqueteAviso decode(FriendlyByteBuf buf) {
        return new PaqueteAviso(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readEnum(TipoAviso.class));
    }

    public static void handle(PaqueteAviso p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DatosCliente.encolarAviso(p)));
        c.setPacketHandled(true);
    }
}
