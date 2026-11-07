package com.senju.core.config;

import com.senju.core.cliente.Posicion;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VISIBLE;
    public static final ForgeConfigSpec.EnumValue<Posicion> POSICION;
    public static final ForgeConfigSpec.IntValue DESPLAZAMIENTO_X;
    public static final ForgeConfigSpec.IntValue DESPLAZAMIENTO_Y;
    public static final ForgeConfigSpec.DoubleValue ESCALA;
    public static final ForgeConfigSpec.BooleanValue FUENTE_PERSONALIZADA;
    public static final ForgeConfigSpec.BooleanValue MAQUINA_ESCRIBIR;
    public static final ForgeConfigSpec.BooleanValue RGB;
    public static final ForgeConfigSpec.DoubleValue VELOCIDAD_RGB;
    public static final ForgeConfigSpec.BooleanValue AVISO_MUERTE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        VISIBLE = b.define("visible", true);
        POSICION = b.defineEnum("posicion", Posicion.ARRIBA_IZQ);
        DESPLAZAMIENTO_X = b.defineInRange("desplazamientoX", 0, -10000, 10000);
        DESPLAZAMIENTO_Y = b.defineInRange("desplazamientoY", 0, -10000, 10000);
        ESCALA = b.defineInRange("escala", 1.0, 0.5, 2.0);
        FUENTE_PERSONALIZADA = b.define("fuentePersonalizada", true);
        MAQUINA_ESCRIBIR = b.define("maquinaEscribir", true);
        RGB = b.define("rgb", true);
        VELOCIDAD_RGB = b.defineInRange("velocidadRgb", 1.0, 0.25, 4.0);
        AVISO_MUERTE = b.comment("Muestra el aviso dracónico al morir, ser eliminado o resucitar alguien").define("avisoMuerte", true);
        SPEC = b.build();
    }

    private ClientConfig() {}
}
