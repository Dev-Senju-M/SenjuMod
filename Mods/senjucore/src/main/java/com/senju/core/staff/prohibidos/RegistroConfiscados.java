package com.senju.core.staff.prohibidos;

import com.senju.core.SenjuCore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.server.MinecraftServer;

/** Escritura del log logs/senjucore-confiscados.log. Nunca lanza excepciones. */
public final class RegistroConfiscados {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RegistroConfiscados() {}

    public static void anotar(MinecraftServer server, String jugador, String uuid, String itemId, int cantidad, String origen) {
        try {
            Path archivo = server.getServerDirectory().toPath().resolve("logs").resolve("senjucore-confiscados.log");
            Files.createDirectories(archivo.getParent());
            String linea = LocalDateTime.now().format(FMT) + " | " + jugador + " (" + uuid + ") | "
                    + itemId + " x" + cantidad + " | " + origen + System.lineSeparator();
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {
            SenjuCore.LOGGER.error("No se pudo escribir senjucore-confiscados.log", ex);
        }
    }
}
