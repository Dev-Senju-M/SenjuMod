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

    public static final ForgeConfigSpec.IntValue VIDAS_INICIALES;
    public static final ForgeConfigSpec.IntValue VIDAS_AL_REVIVIR;
    public static final ForgeConfigSpec.BooleanValue VIDAS_STAFF;
    public static final ForgeConfigSpec.IntValue JUGADORES_RITUAL;
    public static final ForgeConfigSpec.BooleanValue RITUAL_DE_NOCHE;

    public static final ForgeConfigSpec.IntValue AVISO_MINUTOS_ANTES;
    public static final ForgeConfigSpec.IntValue INTERVALO_REVISION;

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
        VIDAS_INICIALES = b.comment("Vidas con las que empieza cada jugador nuevo (no cambia a los existentes)")
                .defineInRange("vidasIniciales", 3, 1, 100);
        VIDAS_AL_REVIVIR = b.comment("Vidas que recibe un jugador resucitado por el ritual")
                .defineInRange("vidasAlRevivir", 1, 1, 100);
        VIDAS_STAFF = b.comment("Si es false, el staff no pierde vidas")
                .define("vidasStaff", true);
        JUGADORES_RITUAL = b.comment("Jugadores vivos necesarios cerca del altar para el ritual")
                .defineInRange("jugadoresRitual", 3, 1, 20);
        RITUAL_DE_NOCHE = b.comment("Si es true, el ritual solo se puede hacer de noche")
                .define("ritualDeNoche", true);
        AVISO_MINUTOS_ANTES = b.comment("Minutos antes de medianoche para avisar a quien no ha cumplido la mision del dia")
                .defineInRange("avisoMinutosAntes", 10, 1, 1440);
        INTERVALO_REVISION = b.comment("Cada cuantos ticks se revisan las misiones de los jugadores")
                .defineInRange("intervaloRevision", 40, 1, 1200);
        SPEC = b.build();
    }

    private CommonConfig() {}
}
