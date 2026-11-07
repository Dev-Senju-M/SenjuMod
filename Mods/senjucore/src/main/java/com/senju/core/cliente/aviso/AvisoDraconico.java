package com.senju.core.cliente.aviso;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.senju.core.cliente.DatosCliente;
import com.senju.core.config.ClientConfig;
import com.senju.core.network.PaqueteAviso;
import com.senju.core.vidas.TipoAviso;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Aviso dracónico a pantalla completa (solo cliente). Saca los avisos de {@link DatosCliente#avisos()} de uno
 * en uno y los dibuja según {@link LineaTiempoAviso}. El tiempo solo avanza mientras se dibuja (pausa, F1 o
 * pantallas que lo ocultan lo congelan), así cada aviso se ve completo y nunca se superponen.
 *
 * <p>Orden de dibujo: velo → brasas → alas → anillo → ojo (iris, pupila, párpado) → nombre (resplandor + texto)
 * → subtítulo → huevos.
 */
public final class AvisoDraconico implements IGuiOverlay {
    private static ResourceLocation tex(String n) {
        return new ResourceLocation("senjucore", "textures/gui/aviso/" + n + ".png");
    }

    private static final ResourceLocation IRIS = tex("ojo_iris");
    private static final ResourceLocation PUPILA = tex("pupila");
    private static final ResourceLocation ANILLO = tex("anillo");
    private static final ResourceLocation ALA = tex("ala");
    public static final ResourceLocation HUEVO = tex("huevo");
    public static final ResourceLocation HUEVO_ROTO = tex("huevo_roto");
    private static final Style ESTILO_TITULO = Style.EMPTY.withFont(new ResourceLocation("senjucore", "aviso"));

    private static final int COLOR_VELO = 0x280000;
    private static final float ALFA_VELO = 0.65f;
    private static final int COLOR_TITULO = 0xFFDDA0, COLOR_RESPLANDOR = 0xFF5A00;
    private static final int COLOR_SUBTITULO = 0xFFB38A, COLOR_PARPADO = 0xFF6A1A;
    private static final int[] COLORES_BRASA = {0xFF7A1A, 0xFFCF4A, 0xFF3D1A};
    private static final int MAX_BRASAS = 80, MAX_HUEVOS = 10;
    /** Tamaño aproximado del contenido central en coordenadas locales (para reducir en pantallas pequeñas). */
    private static final float ANCHO_CONTENIDO = 300f, ALTO_CONTENIDO = 200f;

    private static PaqueteAviso actual;
    private static long transcurrido;
    private static long ultimoFrame = -1;
    private static final List<Brasa> BRASAS = new ArrayList<>();
    private static final Random AZAR = new Random();
    private static float acumuladoBrasas;
    /** Letras del título ya construidas para {@link #tituloDe} (se rehacen solo al cambiar de aviso). */
    private static PaqueteAviso tituloDe;
    private static final List<Component> LETRAS = new ArrayList<>();
    private static final List<Integer> ANCHOS = new ArrayList<>();
    private static int anchoLetras;

    private static final class Brasa {
        float x, y, vy, amplitud, frecuencia, fase, vida;
        int tam, color;
    }

    /** Descarta el aviso actual y las brasas (al desconectarse). La cola la vacía {@link DatosCliente#limpiar()}. */
    public static void limpiar() {
        actual = null;
        transcurrido = 0;
        ultimoFrame = -1;
        BRASAS.clear();
        acumuladoBrasas = 0;
        tituloDe = null;
        LETRAS.clear();
        ANCHOS.clear();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.AVISO_MUERTE.get()) {
            DatosCliente.avisos().clear();
            limpiar();
            return;
        }
        // En la pantalla de muerte se dibuja encima de ella (EventosCliente, ScreenEvent.Render.Post): aquí ni se
        // dibuja ni avanza, y no se toca ultimoFrame para que el reloj siga sin salto.
        if (mc.screen instanceof DeathScreen) return;
        if (mc.options.hideGui || mc.screen instanceof PauseScreen || mc.player == null) {
            ultimoFrame = -1; // congela el tiempo del aviso
            return;
        }
        frame(gfx, w, h);
    }

    /**
     * Avanza el reloj y dibuja un frame del aviso. Lo llaman el overlay (HUD) y, con la pantalla de muerte abierta,
     * el evento de pantalla. Como el overlay no dibuja con la pantalla de muerte, solo uno de los dos corre por frame;
     * aun así, una segunda llamada en el mismo milisegundo no avanzaría (dt = 0).
     */
    public static void frame(GuiGraphics gfx, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.AVISO_MUERTE.get()) {
            DatosCliente.avisos().clear();
            limpiar();
            return;
        }
        long ahora = Util.getMillis();
        if (actual == null) {
            actual = DatosCliente.avisos().pollFirst();
            if (actual == null) {
                ultimoFrame = -1;
                return;
            }
            transcurrido = 0;
            ultimoFrame = ahora;
            BRASAS.clear();
            acumuladoBrasas = 0;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.4f));
        }
        long dt = ultimoFrame < 0 ? 0 : Math.min(100, Math.max(0, ahora - ultimoFrame));
        ultimoFrame = ahora;
        transcurrido += dt;
        long t = transcurrido;
        if (LineaTiempoAviso.terminado(t)) {
            limpiar(); // el siguiente se saca en el próximo frame (empieza desde el velo a 0: sin parpadeo)
            return;
        }
        actualizarBrasas(t, dt / 1000f, w, h);
        dibujar(gfx, mc.font, actual, t, w, h);
    }

    private static void dibujar(GuiGraphics gfx, Font font, PaqueteAviso a, long t, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 300); // por encima de los ítems de la barra rápida

        // 1. Velo rojizo + viñeta arriba/abajo
        float velo = LineaTiempoAviso.velo(t);
        gfx.fill(0, 0, w, h, argb(ALFA_VELO * velo, COLOR_VELO));
        int banda = h / 4;
        gfx.fillGradient(0, 0, w, banda, argb(0.35f * velo, 0), argb(0, 0));
        gfx.fillGradient(0, h - banda, w, h, argb(0, 0), argb(0.35f * velo, 0));

        // 2. Brasas (coordenadas de pantalla)
        for (Brasa b : BRASAS) {
            float alturaRel = Math.min(1f, b.y / (h * 0.35f));
            float alfa = velo * Math.max(0f, alturaRel) * Math.min(1f, b.vida * 3f);
            if (alfa < 0.03f) continue;
            int x = Math.round(b.x + b.amplitud * (float) Math.sin(b.fase + b.vida * b.frecuencia));
            int y = Math.round(b.y);
            gfx.fill(x, y, x + b.tam, y + b.tam, argb(alfa, b.color));
        }

        // Contenido central (escala reducida en pantallas pequeñas)
        float k = Math.min(1f, Math.min(w / ANCHO_CONTENIDO, h / ALTO_CONTENIDO));
        gfx.pose().pushPose();
        gfx.pose().translate(w / 2f, h / 2f, 0);
        gfx.pose().scale(k, k, 1f);

        float emblema = LineaTiempoAviso.emblema(t);
        float ey = -40f;

        // 3. Alas (desde detrás del anillo, se despliegan girando y creciendo). La geometría usa el valor sin
        // fundido (no se pliegan al desvanecerse); el fundido va en la opacidad (emblema).
        float alas = LineaTiempoAviso.alas(t) / Math.max(0.001f, LineaTiempoAviso.fundido(t));
        alas = Math.min(1f, alas);
        if (alas > 0.01f && emblema > 0.01f) {
            float p = Math.max(0f, Math.min(1f, (alas - 0.3f) / 0.7f));
            float giro = 30f * (1f - p);
            RenderSystem.disableCull(); // el ala derecha es la izquierda reflejada
            color(emblema);
            for (int lado = -1; lado <= 1; lado += 2) {
                gfx.pose().pushPose();
                gfx.pose().translate(lado * 30f, ey - 6f, 0);
                gfx.pose().mulPose(Axis.ZP.rotationDegrees(-lado * giro));
                gfx.pose().scale(-lado * 2f * alas, 2f * alas, 1f);
                gfx.blit(ALA, -46, -9, 0, 0, 48, 32, 48, 32);
                gfx.pose().popPose();
            }
            color(1f);
            RenderSystem.enableCull();
        }

        // 4. Anillo de runas girando
        if (emblema > 0.01f) {
            gfx.pose().pushPose();
            gfx.pose().translate(0, ey, 0);
            gfx.pose().mulPose(Axis.ZP.rotationDegrees(t * 0.02f));
            float s = 1.5f * (0.8f + 0.2f * emblema);
            gfx.pose().scale(s, s, 1f);
            color(emblema);
            gfx.blit(ANILLO, -32, -32, 0, 0, 64, 64, 64, 64);
            color(1f);
            gfx.pose().popPose();

            // 5. Ojo: se abre desde una línea escalando en vertical
            boolean eliminado = a.tipo() == TipoAviso.ELIMINADO;
            // aperturaOjo incluye el fundido; se divide para que el ojo no se cierre al desvanecerse.
            float abre = LineaTiempoAviso.aperturaOjo(t) / Math.max(0.001f, LineaTiempoAviso.fundido(t));
            abre = Math.min(1f, abre) * (eliminado ? 0.4f : 1f);
            if (abre > 0.03f) {
                color(emblema);
                gfx.pose().pushPose();
                gfx.pose().translate(0, ey, 0);
                gfx.pose().scale(2f, 2f * abre, 1f);
                gfx.blit(IRIS, -16, -8, 0, 0, 32, 16, 32, 16);
                gfx.pose().popPose();
                gfx.pose().pushPose();
                gfx.pose().translate(0, ey, 0);
                gfx.pose().scale(eliminado ? 0.5f : 2f, 2f * abre, 1f);
                gfx.blit(PUPILA, -2, -8, 0, 0, 4, 16, 4, 16);
                gfx.pose().popPose();
                color(1f);
            }
            float parpado = emblema * (1f - Math.min(1f, abre * 1.5f));
            if (parpado > 0.03f) {
                int yy = Math.round(ey);
                gfx.fill(-30, yy - 1, 30, yy + 1, argb(parpado, COLOR_PARPADO));
                gfx.fill(-20, yy - 1, 20, yy + 1, argb(parpado, 0xFFCF4A));
            }
        }

        // 6. Nombre con letra medieval, resplandor y espaciado que se cierra
        float titulo = LineaTiempoAviso.titulo(t);
        if (titulo > 0.02f) dibujarTitulo(gfx, font, a, titulo, LineaTiempoAviso.espaciadoTitulo(t), 34);

        // 7. Subtítulo
        float sub = LineaTiempoAviso.subtitulo(t);
        if (sub > 0.02f) {
            Component c = switch (a.tipo()) {
                case MUERTE -> Component.translatable("senjucore.aviso.muerte", a.vidas());
                case ELIMINADO -> Component.translatable("senjucore.aviso.eliminado");
                case RESUCITADO -> Component.translatable("senjucore.aviso.resucitado", a.vidas());
            };
            int ancho = font.width(c);
            gfx.drawString(font, c, -ancho / 2, 60, argb(sub, COLOR_SUBTITULO), true);
        }

        // 8. Huevos: N llenos, el resto agrietados
        float huevos = LineaTiempoAviso.huevos(t);
        if (huevos > 0.02f) dibujarHuevos(gfx, font, a, huevos, 76);

        gfx.pose().popPose();
        gfx.pose().popPose();
        color(1f);
        RenderSystem.disableBlend();
    }

    private static void dibujarTitulo(GuiGraphics gfx, Font font, PaqueteAviso a, float alfa, float espaciado, int y) {
        if (tituloDe != a) {
            tituloDe = a;
            LETRAS.clear();
            ANCHOS.clear();
            anchoLetras = 0;
            String nombre = a.nombre();
            for (int i = 0; i < nombre.length(); ) {
                int cp = nombre.codePointAt(i);
                i += Character.charCount(cp);
                Component c = Component.literal(new String(Character.toChars(cp))).withStyle(ESTILO_TITULO);
                int an = font.width(c);
                LETRAS.add(c);
                ANCHOS.add(an);
                anchoLetras += an;
            }
        }
        List<Component> letras = LETRAS;
        List<Integer> anchos = ANCHOS;
        int total = anchoLetras;
        if (letras.isEmpty()) return;
        float totalF = total + espaciado * (letras.size() - 1);
        float s = Math.min(1f, 280f / Math.max(1f, totalF)); // nombres muy largos: se reducen
        gfx.pose().pushPose();
        gfx.pose().translate(0, y, 0);
        gfx.pose().scale(s, s, 1f);
        int glow = argb(0.35f * alfa, COLOR_RESPLANDOR);
        int main = argb(alfa, COLOR_TITULO);
        float x = -totalF / 2f;
        for (int i = 0; i < letras.size(); i++) {
            Component c = letras.get(i);
            int xi = Math.round(x);
            if (alfa * 0.35f > 0.02f) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) gfx.drawString(font, c, xi + dx, dy, glow, false);
                    }
                }
            }
            gfx.drawString(font, c, xi, 0, main, false);
            x += anchos.get(i) + espaciado;
        }
        gfx.pose().popPose();
    }

    private static void dibujarHuevos(GuiGraphics gfx, Font font, PaqueteAviso a, float alfa, int y) {
        int total = Math.max(1, Math.max(a.total(), a.vidas()));
        int llenos = a.tipo() == TipoAviso.ELIMINADO ? 0 : Math.max(0, Math.min(a.vidas(), total));
        color(alfa);
        if (total <= MAX_HUEVOS) {
            int paso = 20; // 16 px de huevo (8×10 a escala 2) + 4 de separación
            int x0 = -(total * paso - 4) / 2;
            for (int i = 0; i < total; i++) {
                gfx.pose().pushPose();
                gfx.pose().translate(x0 + i * paso, y, 0);
                gfx.pose().scale(2f, 2f, 1f);
                gfx.blit(i < llenos ? HUEVO : HUEVO_ROTO, 0, 0, 0, 0, 8, 10, 8, 10);
                gfx.pose().popPose();
            }
            color(1f);
        } else {
            // Muchas vidas: un huevo y el número.
            String txt = llenos + " / " + total;
            int ancho = 16 + 4 + font.width(txt);
            int x0 = -ancho / 2;
            gfx.pose().pushPose();
            gfx.pose().translate(x0, y, 0);
            gfx.pose().scale(2f, 2f, 1f);
            gfx.blit(llenos > 0 ? HUEVO : HUEVO_ROTO, 0, 0, 0, 0, 8, 10, 8, 10);
            gfx.pose().popPose();
            color(1f);
            gfx.drawString(font, txt, x0 + 20, y + 6, argb(alfa, COLOR_SUBTITULO), true);
        }
    }

    private static void actualizarBrasas(long t, float dt, int w, int h) {
        if (dt <= 0) return;
        for (int i = BRASAS.size() - 1; i >= 0; i--) {
            Brasa b = BRASAS.get(i);
            b.y -= b.vy * dt;
            b.vida += dt;
            if (b.y < -4) BRASAS.remove(i);
        }
        if (t >= 5000) return; // durante el fundido ya no nacen
        acumuladoBrasas += dt * 28f; // ~28 por segundo
        while (acumuladoBrasas >= 1f) {
            acumuladoBrasas -= 1f;
            if (BRASAS.size() >= MAX_BRASAS) continue;
            Brasa b = new Brasa();
            b.x = AZAR.nextFloat() * w;
            b.y = h + AZAR.nextFloat() * 6f;
            b.vy = 18f + AZAR.nextFloat() * 34f;
            b.amplitud = 2f + AZAR.nextFloat() * 6f;
            b.frecuencia = 1.5f + AZAR.nextFloat() * 2.5f;
            b.fase = AZAR.nextFloat() * 6.283f;
            b.tam = 1 + AZAR.nextInt(3);
            b.color = COLORES_BRASA[AZAR.nextInt(COLORES_BRASA.length)];
            BRASAS.add(b);
        }
    }

    /**
     * Prepara un grupo de blits con opacidad {@code alfa}. Activa la mezcla cada vez: los fill/drawString previos
     * vacían {@code RenderType.gui()}/texto y su estado de transparencia la desactiva al terminar.
     */
    private static void color(float alfa) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alfa)));
    }

    /** ARGB con alfa en [0,1]. Ojo: el texto con alfa < 4/255 se dibuja opaco, por eso se omite con alfa ≤ 0.02. */
    private static int argb(float alfa, int rgb) {
        int a = Math.round(Math.max(0f, Math.min(1f, alfa)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
