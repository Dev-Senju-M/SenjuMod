package com.senju.core.staff.castigos;

import com.senju.core.SenjuCore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.server.MinecraftServer;

/** Escritura de logs/senjucore-castigos.log. Nunca lanza excepciones. */
public final class RegistroLogCastigos {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RegistroLogCastigos() {}

    /** accion: APLICAR o PERDONAR. */
    public static void anotar(MinecraftServer server, String staff, String accion, String jugador, String uuid,
            String castigo, String duracion, String motivo) {
        try {
            Path archivo = server.getServerDirectory().toPath().resolve("logs").resolve("senjucore-castigos.log");
            Files.createDirectories(archivo.getParent());
            String linea = LocalDateTime.now().format(FMT) + " | " + limpiar(staff) + " | " + accion + " | "
                    + limpiar(jugador) + " (" + uuid + ") | " + castigo + " | " + duracion + " | " + limpiar(motivo)
                    + System.lineSeparator();
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {
            SenjuCore.LOGGER.error("No se pudo escribir senjucore-castigos.log", ex);
        }
    }

    /** Evita que un motivo con saltos de línea o barras verticales rompa el formato de una línea. */
    private static String limpiar(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
