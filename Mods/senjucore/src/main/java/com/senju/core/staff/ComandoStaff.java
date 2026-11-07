package com.senju.core.staff;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.senju.core.staff.castigos.Castigos;
import com.senju.core.staff.castigos.Duracion;
import com.senju.core.staff.castigos.RegistroCastigos;
import com.senju.core.staff.castigos.TipoCastigo;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** /staff placa [jugador]. */
public class ComandoStaff {
    @SubscribeEvent
    public void alRegistrar(RegisterCommandsEvent ev) {
        registrar(ev.getDispatcher());
    }

    static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("staff")
                .requires(Staff::esStaff)
                .then(Commands.literal("perdonar")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .executes(c -> perdonar(c, null))
                                .then(perdonarTipo(TipoCastigo.SILENCIO))
                                .then(perdonarTipo(TipoCastigo.BAN))
                                .then(perdonarTipo(TipoCastigo.CONGELAR))
                                .then(perdonarTipo(TipoCastigo.CARCEL))))
                .then(Commands.literal("historial")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .executes(ComandoStaff::historial)))
                .then(Commands.literal("carcel")
                        .then(Commands.literal("fijar").executes(ComandoStaff::fijarCarcel)))
                .then(Commands.literal("banear")
                        .then(Commands.argument("jugador", GameProfileArgument.gameProfile())
                                .then(Commands.argument("duracion", StringArgumentType.word())
                                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                                .executes(ComandoStaff::banear)))))
                .then(Commands.literal("vanish")
                        .executes(c -> {
                            com.senju.core.staff.vanish.Vanish.alternar(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("placa")
                        .executes(c -> dar(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(c -> dar(c, EntityArgument.getPlayer(c, "jugador"))))));
    }

    private static boolean tienePlaca(ServerPlayer p) {
        if (PlacaStaffItem.esPlaca(p.containerMenu.getCarried())) return true;
        for (ItemStack s : p.getInventory().items) if (PlacaStaffItem.esPlaca(s)) return true;
        for (ItemStack s : p.getInventory().offhand) if (PlacaStaffItem.esPlaca(s)) return true;
        for (ItemStack s : p.getInventory().armor) if (PlacaStaffItem.esPlaca(s)) return true;
        return false;
    }

    private static int dar(CommandContext<CommandSourceStack> c, ServerPlayer objetivo)
            throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        String nombre = objetivo.getGameProfile().getName();
        if (!Staff.esStaff(objetivo)) {
            src.sendFailure(Component.translatable("senjucore.placa.noesstaff", nombre));
            return 0;
        }
        if (tienePlaca(objetivo)) {
            src.sendFailure(Component.translatable("senjucore.placa.yatiene", nombre));
            return 0;
        }
        // No usar Inventory.add(): en creativo pone el conteo a 0 y devuelve true sin dar nada.
        int libre = objetivo.getInventory().getFreeSlot();
        if (libre < 0) {
            src.sendFailure(Component.translatable("senjucore.placa.lleno", nombre));
            return 0;
        }
        objetivo.getInventory().setItem(libre, new ItemStack(ItemsMod.PLACA_STAFF.get()));
        src.sendSuccess(() -> Component.translatable("senjucore.placa.dada", nombre), true);
        return 1;
    }

    // ---------------------------------------------------------------- castigos

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static LiteralArgumentBuilder<CommandSourceStack> perdonarTipo(TipoCastigo t) {
        return Commands.literal(t.clave()).executes(c -> perdonar(c, t));
    }

    private static int perdonar(CommandContext<CommandSourceStack> c, TipoCastigo tipo)
            throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        int n = 0;
        for (GameProfile gp : GameProfileArgument.getGameProfiles(c, "jugador")) {
            String nombre = nombreDe(gp);
            if (Castigos.perdonar(src, gp, tipo)) {
                n++;
                src.sendSuccess(() -> Component.translatable("senjucore.comando.perdonado", nombre), false);
            } else {
                src.sendFailure(Component.translatable("senjucore.comando.sinperdon", nombre));
            }
        }
        return n;
    }

    private static int historial(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        int total = 0;
        for (GameProfile gp : GameProfileArgument.getGameProfiles(c, "jugador")) {
            String nombre = nombreDe(gp);
            List<RegistroCastigos.Entrada> h = gp.getId() == null
                    ? List.of() : Castigos.historial(src.getServer(), gp.getId(), 10);
            if (h.isEmpty()) {
                src.sendSuccess(() -> Component.translatable("senjucore.comando.historial.vacio", nombre), false);
                continue;
            }
            src.sendSuccess(() -> Component.translatable("senjucore.comando.historial.titulo", nombre), false);
            for (RegistroCastigos.Entrada e : h) {
                src.sendSuccess(() -> Component.translatable("senjucore.comando.historial.linea",
                        FECHA.format(Instant.ofEpochMilli(e.fecha())), accionTexto(e.accion()),
                        Component.translatable("senjucore.castigo.tipo." + e.tipo().clave()),
                        e.duracion(), e.motivo(), e.staff()), false);
            }
            total += h.size();
        }
        return total;
    }

    private static int fijarCarcel(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel nivel = p.serverLevel();
        RegistroCastigos.Posicion pos = new RegistroCastigos.Posicion(nivel.dimension().location().toString(),
                p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        RegistroCastigos.de(src.getServer()).fijarCarcel(pos);
        src.sendSuccess(() -> Component.translatable("senjucore.comando.carcel.fijada", pos.dimension(),
                String.format("%.1f", pos.x()), String.format("%.1f", pos.y()), String.format("%.1f", pos.z())),
                true);
        return 1;
    }

    private static int banear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        String durTxt = StringArgumentType.getString(c, "duracion");
        OptionalLong dur = Duracion.parsear(durTxt);
        if (dur.isEmpty()) {
            src.sendFailure(Component.translatable("senjucore.comando.banear.duracion", durTxt));
            return 0;
        }
        String motivo = StringArgumentType.getString(c, "motivo").trim();
        if (motivo.isEmpty()) {
            src.sendFailure(Component.translatable("senjucore.comando.banear.sinmotivo"));
            return 0;
        }
        ServerPlayer actor = src.getEntity() instanceof ServerPlayer sp ? sp : null;
        Component durComp = dur.getAsLong() == Duracion.PERMANENTE
                ? Component.translatable("senjucore.castigo.permanente")
                : Component.literal(Duracion.formatoDuracion(dur.getAsLong()));
        int n = 0;
        Collection<GameProfile> perfiles = GameProfileArgument.getGameProfiles(c, "jugador");
        if (perfiles.size() != 1) {
            src.sendFailure(Component.translatable("senjucore.comando.banear.varios"));
            return 0;
        }
        for (GameProfile gp : perfiles) {
            String nombre = nombreDe(gp);
            Castigos.Resultado r = Castigos.aplicar(src.getServer(), src.getTextName(), actor, gp,
                    TipoCastigo.BAN, dur.getAsLong(), motivo);
            switch (r) {
                case OK -> {
                    n++;
                    src.sendSuccess(() -> Component.translatable("senjucore.comando.banear.ok", nombre, durComp,
                            motivo), false);
                }
                case ES_STAFF -> {
                    // Con jugador, aplicar ya le avisó; la consola necesita el aviso aquí.
                    if (actor == null) src.sendFailure(Component.translatable("senjucore.castigo.esstaff"));
                }
                default -> src.sendFailure(Component.translatable("senjucore.comando.banear.fallo", nombre));
            }
        }
        return n;
    }

    private static Component accionTexto(String accion) {
        return switch (accion) {
            case "APLICAR", "PERDONAR", "VENCER" ->
                    Component.translatable("senjucore.comando.historial.accion." + accion.toLowerCase());
            default -> Component.literal(accion);
        };
    }

    private static String nombreDe(GameProfile gp) {
        return gp.getName() != null ? gp.getName() : String.valueOf(gp.getId());
    }
}
