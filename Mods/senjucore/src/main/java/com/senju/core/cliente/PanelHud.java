package com.senju.core.cliente;

import com.senju.core.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.time.LocalTime;

/** Panel HUD con los datos del jugador. Solo cliente. */
public final class PanelHud implements IGuiOverlay {
    private static final int PAD = 4, MARGEN = 4, FONDO = 0x80000000, GRIS = 0xAAAAAA;
    private static final int ALTO_LINEA = 10;

    @Override
    public void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.VISIBLE.get() || mc.options.hideGui || mc.player == null) return;
        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) return;

        Font font = mc.font;
        String servidor = DatosCliente.nombreServidor();
        if (DatosCliente.sinLimite() && mc.getSingleplayerServer() != null) {
            servidor = mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        String nombre = mc.player.getGameProfile().getName();
        String tiempo = null;
        int colorTiempo = 0xFFFFFF;
        if (DatosCliente.tieneDatos()) {
            if (DatosCliente.sinLimite()) {
                tiempo = I18n.get("senjucore.panel.jugado", Formato.duracion(DatosCliente.usadosHoy()));
            } else {
                tiempo = I18n.get("senjucore.panel.restante", Formato.duracion(DatosCliente.restantes()));
                colorTiempo = Formato.colorTiempo(DatosCliente.restantes());
            }
        }
        String hora = I18n.get("senjucore.panel.hora", Formato.hora12(LocalTime.now()));
        String misiones = "Misiones: Pr\u00f3ximamente";

        int ancho = Math.max(font.width(servidor), 10 + font.width(nombre));
        if (tiempo != null) ancho = Math.max(ancho, font.width(tiempo));
        ancho = Math.max(ancho, Math.max(font.width(hora), font.width(misiones))) + 2 * PAD;
        int lineas = tiempo != null ? 5 : 4;
        int alto = lineas * ALTO_LINEA + 2 * PAD - 2;

        float escala = ClientConfig.ESCALA.get().floatValue();
        int[] o = ClientConfig.POSICION.get().origen(Math.round(w / escala), Math.round(h / escala),
                ancho, alto, MARGEN, ClientConfig.DESPLAZAMIENTO_X.get(), ClientConfig.DESPLAZAMIENTO_Y.get());

        gfx.pose().pushPose();
        gfx.pose().scale(escala, escala, 1f);
        int x = o[0] + PAD, y = o[1] + PAD;
        gfx.fill(o[0], o[1], o[0] + ancho, o[1] + alto, FONDO);
        gfx.drawString(font, servidor, x, y, 0xFFFFFF);
        y += ALTO_LINEA;
        PlayerFaceRenderer.draw(gfx, mc.player.getSkinTextureLocation(), x, y, 8);
        gfx.drawString(font, nombre, x + 10, y, 0xFFFFFF);
        y += ALTO_LINEA;
        if (tiempo != null) {
            gfx.drawString(font, tiempo, x, y, colorTiempo);
            y += ALTO_LINEA;
        }
        gfx.drawString(font, hora, x, y, 0xFFFFFF);
        y += ALTO_LINEA;
        gfx.drawString(font, misiones, x, y, GRIS);
        gfx.pose().popPose();
    }
}
