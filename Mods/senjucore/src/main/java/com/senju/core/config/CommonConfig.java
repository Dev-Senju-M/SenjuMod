package com.senju.core.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> NOMBRE_SERVIDOR;
    public static final ForgeConfigSpec.DoubleValue HORAS_DIARIAS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> AVISOS_MINUTOS;
    public static final ForgeConfigSpec.BooleanValue EXIMIR_OPERADORES;
    public static final ForgeConfigSpec.BooleanValue STAFF_INCLUYE_OPERADORES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STAFF;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOTIVOS;

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
        STAFF_INCLUYE_OPERADORES = b.comment("Si es true, los operadores (nivel 2+) cuentan como staff")
                .define("staffIncluyeOperadores", true);
        STAFF = b.comment("Nombres de jugadores que cuentan como staff (sin distinguir mayusculas)")
                .defineList("staff", List.of(), o -> o instanceof String);
        MOTIVOS = b.comment("Motivos predefinidos para los castigos (botones del panel)")
                .defineList("motivos", List.of("Lenguaje ofensivo", "Spam", "Hacks o trampas", "Grief", "Acoso", "Otro"),
                        o -> o instanceof String s && !s.isBlank());
        SPEC = b.build();
    }

    private CommonConfig() {}
}
