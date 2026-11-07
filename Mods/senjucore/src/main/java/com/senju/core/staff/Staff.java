package com.senju.core.staff;

import com.senju.core.config.CommonConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Puente entre Minecraft/configuración y ReglaStaff. */
public final class Staff {
    private Staff() {}

    public static boolean esStaff(ServerPlayer p) {
        return ReglaStaff.esStaff(p.hasPermissions(2), p.getGameProfile().getName(),
                CommonConfig.STAFF.get(), CommonConfig.STAFF_INCLUYE_OPERADORES.get());
    }

    public static boolean esStaff(CommandSourceStack s) {
        if (s.getEntity() instanceof ServerPlayer p) return esStaff(p);
        return s.hasPermission(4);
    }
}
