package com.senju.core.cliente;

import com.senju.core.config.ClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.time.LocalTime;

/** Panel HUD con los datos del jugador. Solo cliente. */
public final class PanelHud implements IGuiOverlay {
    private static final int PAD = 4, MARGEN = 4, FONDO = 0x80000000, GRIS = 0xAAAAAA;
    private static final int ALTO_LINEA = 10;
    private static final Style ESTILO_PANEL = Style.EMPTY.withFont(new ResourceLocation("senjucore", "panel"));

    @Override
    public void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.VISIBLE.get() || mc.options.hideGui || mc.player == null) return;
        if (mc.options.renderDebug) return;
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
        String misiones = I18n.get("senjucore.panel.misiones");

        Style estilo = ClientConfig.FUENTE_PERSONALIZADA.get() ? ESTILO_PANEL : Style.EMPTY;
        boolean escribir = ClientConfig.MAQUINA_ESCRIBIR.get();
        boolean rgb = ClientConfig.RGB.get();
        double vel = ClientConfig.VELOCIDAD_RGB.get();
        long ms = Util.getMillis();

        Component cNombre = txt(nombre, estilo);
        Component cTiempo = tiempo != null ? txt(tiempo, estilo) : null;
        Component cHora = txt(hora, estilo);
        Component cMisiones = txt(misiones, estilo);

        // El ancho se calcula con el nombre completo (+ cursor) para que no varíe con la animación.
        int anchoServidor = font.width(txt(escribir ? servidor + "_" : servidor, estilo));
        int ancho = Math.max(anchoServidor, 10 + font.width(cNombre));
        if (cTiempo != null) ancho = Math.max(ancho, font.width(cTiempo));
        ancho = Math.max(ancho, Math.max(font.width(cHora), font.width(cMisiones))) + 2 * PAD;
        int lineas = tiempo != null ? 5 : 4;
        int alto = lineas * ALTO_LINEA + 2 * PAD - 2;

        float escala = ClientConfig.ESCALA.get().floatValue();
        int[] o = ClientConfig.POSICION.get().origen((int) Math.floor(w / escala), (int) Math.floor(h / escala),
                ancho, alto, MARGEN, ClientConfig.DESPLAZAMIENTO_X.get(), ClientConfig.DESPLAZAMIENTO_Y.get());

        gfx.pose().pushPose();
        gfx.pose().scale(escala, escala, 1f);
        int x = o[0] + PAD, y = o[1] + PAD;
        gfx.fill(o[0], o[1], o[0] + ancho, o[1] + alto, FONDO);
        dibujarServidor(gfx, font, servidor, estilo, escribir, rgb, vel, ms, x, y);
        y += ALTO_LINEA;
        PlayerFaceRenderer.draw(gfx, mc.player.getSkinTextureLocation(), x, y, 8);
        int linea = 1;
        gfx.drawString(font, cNombre, x + 10, y, rgb ? Efectos.colorRgb(ms, vel, linea * Efectos.DESFASE_LINEA) : 0xFFFFFF);
        y += ALTO_LINEA;
        linea++;
        if (cTiempo != null) {
            boolean parpadea = rgb && !DatosCliente.sinLimite() && DatosCliente.restantes() < 600;
            if (!parpadea || Efectos.parpadeoVisible(ms)) {
                gfx.drawString(font, cTiempo, x, y, rgb ? Efectos.colorRgb(ms, vel, linea * Efectos.DESFASE_LINEA) : colorTiempo);
            }
            y += ALTO_LINEA;
            linea++;
        }
        gfx.drawString(font, cHora, x, y, rgb ? Efectos.colorRgb(ms, vel, linea * Efectos.DESFASE_LINEA) : 0xFFFFFF);
        y += ALTO_LINEA;
        linea++;
        gfx.drawString(font, cMisiones, x, y, rgb ? Efectos.colorRgb(ms, vel, linea * Efectos.DESFASE_LINEA) : GRIS);
        gfx.pose().popPose();
    }

    private static Component txt(String s, Style estilo) {
        return Component.literal(s).withStyle(estilo);
    }

    private static void dibujarServidor(GuiGraphics gfx, Font font, String servidor, Style estilo, boolean escribir,
                                        boolean rgb, double vel, long ms, int x, int y) {
        int n = servidor.length();
        int visibles = escribir ? Efectos.letrasVisibles(ms, n) : n;
        if (!rgb) {
            String texto = servidor.substring(0, visibles);
            if (escribir && Efectos.cursorVisible(ms)) texto += "_";
            if (!texto.isEmpty()) gfx.drawString(font, txt(texto, estilo), x, y, 0xFFFFFF);
            return;
        }
        int cx = x;
        for (int k = 0; k < visibles; k++) {
            Component c = txt(String.valueOf(servidor.charAt(k)), estilo);
            gfx.drawString(font, c, cx, y, Efectos.colorRgb(ms, vel, k * Efectos.DESFASE_LETRA));
            cx += font.width(c);
        }
        if (escribir && Efectos.cursorVisible(ms)) {
            gfx.drawString(font, txt("_", estilo), cx, y, Efectos.colorRgb(ms, vel, visibles * Efectos.DESFASE_LETRA));
        }
    }
}
