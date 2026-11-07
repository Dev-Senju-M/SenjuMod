package com.senju.core.staff;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
}
