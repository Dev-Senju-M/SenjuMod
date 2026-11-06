package com.senju.core.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> NOMBRE_SERVIDOR;
    public static final ForgeConfigSpec.DoubleValue HORAS_DIARIAS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> AVISOS_MINUTOS;
    public static final ForgeConfigSpec.BooleanValue EXIMIR_OPERADORES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        NOMBRE_SERVIDOR = b.comment("Nombre del servidor mostrado en el panel")
                .define("nombreServidor", "Mi Servidor");
        HORAS_DIARIAS = b.comment("Horas de juego permitidas por dia")
                .defineInRange("horasDiarias", 6.0, 0.01, 24.0);
        AVISOS_MINUTOS = b.comment("Minutos restantes en los que se avisa al jugador")
                .defineList("avisosMinutos", List.of(10, 1), o -> o instanceof Integer i && i > 0);
        EXIMIR_OPERADORES = b.comment("Si es true, los operadores no tienen limite diario")
                .define("eximirOperadores", false);
        SPEC = b.build();
    }

    private CommonConfig() {}
}
