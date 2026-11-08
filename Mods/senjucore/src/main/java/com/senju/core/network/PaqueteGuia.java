package com.senju.core.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.senju.core.cliente.guia.GuiaCliente;
import com.senju.core.vidas.altar.EstadoGuia.Esperado;
import com.senju.core.vidas.altar.EstadoGuia.EstadoPos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Guía de construcción del altar (S2C). {@code activa=false} la limpia en el cliente. */
public record PaqueteGuia(BlockPos altar, List<Entrada> entradas, boolean activa) {
    public static final int MAX_ENTRADAS = 12;

    /** Posición relativa al altar, lo esperado y su estado actual. */
    public record Entrada(byte dx, byte dy, byte dz, Esperado esperado, EstadoPos estado) {}

    public static void encode(PaqueteGuia p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.altar);
        int n = Math.min(p.entradas.size(), MAX_ENTRADAS);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            Entrada e = p.entradas.get(i);
            buf.writeByte(e.dx);
            buf.writeByte(e.dy);
            buf.writeByte(e.dz);
            buf.writeByte(e.esperado.ordinal());
            buf.writeByte(e.estado.ordinal());
        }
        buf.writeBoolean(p.activa);
    }

    public static PaqueteGuia decode(FriendlyByteBuf buf) {
        BlockPos altar = buf.readBlockPos();
        int n = Math.min(buf.readVarInt(), MAX_ENTRADAS);
        List<Entrada> l = new ArrayList<>(n);
        Esperado[] es = Esperado.values();
        EstadoPos[] st = EstadoPos.values();
        for (int i = 0; i < n; i++) {
            byte dx = buf.readByte(), dy = buf.readByte(), dz = buf.readByte();
            int e = buf.readByte(), s = buf.readByte();
            if (e < 0 || e >= es.length || s < 0 || s >= st.length) continue;
            l.add(new Entrada(dx, dy, dz, es[e], st[s]));
        }
        return new PaqueteGuia(altar, l, buf.readBoolean());
    }

    public static void handle(PaqueteGuia p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                GuiaCliente.recibir(p.altar, p.entradas, p.activa)));
        c.setPacketHandled(true);
    }
}
