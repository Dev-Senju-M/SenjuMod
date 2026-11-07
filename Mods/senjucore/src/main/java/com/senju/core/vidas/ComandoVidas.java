package com.senju.core.vidas;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

/** Subcomandos de staff: {@code vidas} y {@code revivir} (se cuelgan de /staff, que ya exige ser staff). */
public final class ComandoVidas {
    private ComandoVidas() {}

    public static LiteralArgumentBuilder<CommandSourceStack> vidas() {
        return Commands.literal("vidas")
                .then(Commands.literal("fijar")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 100))
                                        .executes(ComandoVidas::fijar))))
                .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                        .executes(ComandoVidas::ver));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> revivir() {
        return Commands.literal("revivir")
                .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 100))
                                .executes(ComandoVidas::revivir)));
    }

    /** Exactamente un perfil con UUID, o null tras avisar del error. */
    private static GameProfile unPerfil(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<GameProfile> perfiles = GameProfileArgument.getGameProfiles(c, "jugador");
        if (perfiles.size() != 1) {
            c.getSource().sendFailure(Component.translatable("senjucore.vidas.unjugador"));
            return null;
        }
        GameProfile gp = perfiles.iterator().next();
        if (gp.getId() == null) {
            c.getSource().sendFailure(Component.translatable("senjucore.vidas.unjugador"));
            return null;
        }
        return gp;
    }

    private static String nombreDe(GameProfile gp) {
        return gp.getName() != null ? gp.getName() : String.valueOf(gp.getId());
    }

    private static int ver(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        GameProfile gp = unPerfil(c);
        if (gp == null) return 0;
        CommandSourceStack src = c.getSource();
        String nombre = nombreDe(gp);
        RegistroVidas.Datos d = RegistroVidas.de(src.getServer()).datos(gp.getId());
        if (d.pendienteRevivir > 0) {
            int n = d.pendienteRevivir;
            src.sendSuccess(() -> Component.translatable("senjucore.vidas.ver.pendiente", nombre, n), false);
        } else if (ReglaVidas.eliminado(d.vidas)) {
            src.sendSuccess(() -> Component.translatable("senjucore.vidas.ver.eliminado", nombre), false);
        } else {
            int v = d.vidas;
            src.sendSuccess(() -> Component.translatable("senjucore.vidas.ver", nombre, v), false);
        }
        return d.vidas;
    }

    private static int fijar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        GameProfile gp = unPerfil(c);
        if (gp == null) return 0;
        int n = IntegerArgumentType.getInteger(c, "n");
        Vidas.fijar(c.getSource().getServer(), gp, n);
        String nombre = nombreDe(gp);
        c.getSource().sendSuccess(() -> Component.translatable("senjucore.vidas.fijado", nombre, n), true);
        return n;
    }

    private static int revivir(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        GameProfile gp = unPerfil(c);
        if (gp == null) return 0;
        CommandSourceStack src = c.getSource();
        String nombre = nombreDe(gp);
        RegistroVidas reg = RegistroVidas.de(src.getServer());
        if (!reg.eliminado(gp.getId())) {
            src.sendFailure(Component.translatable("senjucore.vidas.noeliminado", nombre));
            return 0;
        }
        int n = IntegerArgumentType.getInteger(c, "n");
        boolean conectado = src.getServer().getPlayerList().getPlayer(gp.getId()) != null;
        Vidas.revivir(src.getServer(), gp, n, null);
        src.sendSuccess(() -> Component.translatable(
                conectado ? "senjucore.vidas.revivido" : "senjucore.vidas.revivido.pendiente", nombre, n), true);
        return n;
    }
}
