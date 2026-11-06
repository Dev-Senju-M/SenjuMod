package com.senju.core.tiempo;

import java.util.Collection;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.senju.core.cliente.Formato;
import com.senju.core.config.CommonConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** /tiempo ver|reiniciar|dar para operadores. */
public class ComandoTiempo {
    @SubscribeEvent
    public void alRegistrar(RegisterCommandsEvent ev) {
        registrar(ev.getDispatcher());
    }

    static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tiempo")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("ver")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .executes(ComandoTiempo::ver)))
                .then(Commands.literal("reiniciar")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .executes(ComandoTiempo::reiniciar)))
                .then(Commands.literal("dar")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 1440))
                                        .executes(ComandoTiempo::dar)))));
    }

    private static long limite() {
        return ReglaTiempo.limiteSegundos(CommonConfig.HORAS_DIARIAS.get());
    }

    /** Estado del jugador normalizado a hoy y guardado. */
    private static EstadoTiempo estadoHoy(MinecraftServer server, UUID id) {
        RegistroTiempo reg = RegistroTiempo.de(server);
        EstadoTiempo e = ReglaTiempo.normalizar(reg.get(id), ControlTiempo.hoy());
        reg.set(id, e);
        return e;
    }

    private static void sincronizarSiEnLinea(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) ControlTiempo.sincronizar(p);
    }

    private static int ver(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Collection<GameProfile> perfiles = GameProfileArgument.getGameProfiles(c, "jugador");
        for (GameProfile gp : perfiles) {
            EstadoTiempo e = estadoHoy(server, gp.getId());
            c.getSource().sendSuccess(() -> Component.translatable("senjucore.cmd.ver",
                    gp.getName(),
                    Formato.duracion(e.usados()),
                    Formato.duracion(ReglaTiempo.restantes(e, limite()))), false);
        }
        return perfiles.size();
    }

    private static int reiniciar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Collection<GameProfile> perfiles = GameProfileArgument.getGameProfiles(c, "jugador");
        for (GameProfile gp : perfiles) {
            RegistroTiempo.de(server).set(gp.getId(), new EstadoTiempo(ControlTiempo.hoy(), 0));
            sincronizarSiEnLinea(server, gp.getId());
            c.getSource().sendSuccess(() -> Component.translatable("senjucore.cmd.reiniciar",
                    gp.getName()), true);
        }
        return perfiles.size();
    }

    private static int dar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        Collection<GameProfile> perfiles = GameProfileArgument.getGameProfiles(c, "jugador");
        int min = IntegerArgumentType.getInteger(c, "minutos");
        for (GameProfile gp : perfiles) {
            EstadoTiempo e = ReglaTiempo.dar(estadoHoy(server, gp.getId()), min);
            RegistroTiempo.de(server).set(gp.getId(), e);
            sincronizarSiEnLinea(server, gp.getId());
            c.getSource().sendSuccess(() -> Component.translatable("senjucore.cmd.dar",
                    min, gp.getName(),
                    Formato.duracion(ReglaTiempo.restantes(e, limite()))), true);
        }
        return perfiles.size();
    }
}
