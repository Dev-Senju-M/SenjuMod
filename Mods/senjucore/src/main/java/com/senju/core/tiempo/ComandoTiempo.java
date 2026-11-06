package com.senju.core.tiempo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.senju.core.cliente.Formato;
import com.senju.core.config.CommonConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
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
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ComandoTiempo::ver)))
                .then(Commands.literal("reiniciar")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ComandoTiempo::reiniciar)))
                .then(Commands.literal("dar")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 1440))
                                        .executes(ComandoTiempo::dar)))));
    }

    private static long limite() {
        return ReglaTiempo.limiteSegundos(CommonConfig.HORAS_DIARIAS.get());
    }

    /** Estado del jugador normalizado a hoy y guardado. */
    private static EstadoTiempo estadoHoy(ServerPlayer p) {
        RegistroTiempo reg = RegistroTiempo.de(p.server);
        EstadoTiempo e = ReglaTiempo.normalizar(reg.get(p.getUUID()), ControlTiempo.hoy());
        reg.set(p.getUUID(), e);
        return e;
    }

    private static int ver(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
        EstadoTiempo e = estadoHoy(p);
        c.getSource().sendSuccess(() -> Component.translatable("senjucore.cmd.ver",
                p.getGameProfile().getName(),
                Formato.duracion(e.usados()),
                Formato.duracion(ReglaTiempo.restantes(e, limite()))), false);
        return 1;
    }

    private static int reiniciar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
        RegistroTiempo.de(p.server).set(p.getUUID(), new EstadoTiempo(ControlTiempo.hoy(), 0));
        ControlTiempo.sincronizar(p);
        c.getSource().sendSuccess(() -> Component.translatable("senjucore.cmd.reiniciar",
                p.getGameProfile().getName()), true);
        return 1;
    }

    private static int dar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
        int min = IntegerArgumentType.getInteger(c, "minutos");
        EstadoTiempo e = ReglaTiempo.dar(estadoHoy(p), min);
        RegistroTiempo.de(p.server).set(p.getUUID(), e);
        ControlTiempo.sincronizar(p);
        c.getSource().sendSuccess(() -> Component.translatable("senjucore.cmd.dar",
                min, p.getGameProfile().getName(),
                Formato.duracion(ReglaTiempo.restantes(e, limite()))), true);
        return 1;
    }
}
