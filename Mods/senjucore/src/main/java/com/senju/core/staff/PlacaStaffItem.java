package com.senju.core.staff;

import java.util.List;
import com.senju.core.staff.menu.MenuStaff;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Herramienta exclusiva del staff. */
public class PlacaStaffItem extends Item {
    public PlacaStaffItem(Properties props) {
        super(props);
    }

    public static boolean esPlaca(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof PlacaStaffItem;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("senjucore.placa.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("senjucore.placa.desc2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        // El cliente ya quitó el ítem antes de enviar DROP_ITEM; se fuerza el reenvío del slot.
        if (player instanceof ServerPlayer sp) {
            Inventory inv = sp.getInventory();
            sp.containerMenu.findSlot(inv, inv.selected)
                    .ifPresent(i -> sp.containerMenu.setRemoteSlot(i, ItemStack.EMPTY));
            sp.inventoryMenu.findSlot(inv, inv.selected)
                    .ifPresent(i -> sp.inventoryMenu.setRemoteSlot(i, ItemStack.EMPTY));
        }
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false; // ni shulker boxes ni bundles
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp && Staff.esStaff(sp)) {
            MenuStaff.abrir(sp);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        return InteractionResultHolder.pass(stack);
    }
}
