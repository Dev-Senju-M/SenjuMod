package com.senju.core.cliente.guia;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.senju.core.SenjuCore;
import com.senju.core.network.PaqueteGuia;
import com.senju.core.vidas.altar.EstadoGuia.Esperado;
import com.senju.core.vidas.altar.EstadoGuia.EstadoPos;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dibuja la guía del altar: bloques fantasma translúcidos que laten donde falta obsidiana o
 * antorcha de almas, contorno ámbar en esos huecos y contorno rojo donde hay un bloque equivocado.
 */
@Mod.EventBusSubscriber(modid = SenjuCore.MODID, value = Dist.CLIENT)
public final class RenderGuia {
    private RenderGuia() {}

    /** Periodo del latido en milisegundos. */
    private static final double PERIODO_MS = 1500.0;
    private static final float ESCALA = 0.98f;
    private static final double INFLAR = 0.002;
    /** Caja de la antorcha (forma de la antorcha de pie, en coordenadas de bloque). */
    private static final AABB CAJA_ANTORCHA = new AABB(6 / 16.0, 0, 6 / 16.0, 10 / 16.0, 10 / 16.0, 10 / 16.0);
    private static final AABB CAJA_BLOQUE = new AABB(0, 0, 0, 1, 1, 1);

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!GuiaCliente.activa()) return;
        BlockPos altar = GuiaCliente.altar();
        List<PaqueteGuia.Entrada> entradas = GuiaCliente.entradas();
        if (altar == null || entradas.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher bloques = mc.getBlockRenderer();
        PoseStack pose = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();

        double t = (Util.getMillis() % (long) PERIODO_MS) / PERIODO_MS * Math.PI * 2.0;
        float alfa = (float) (0.35 + 0.2 * Math.sin(t));
        // Todo lo que el modelo pida va a translúcido, con el alfa del vértice multiplicado.
        MultiBufferSource translucido = rt -> new AlfaConsumer(buffers.getBuffer(RenderType.translucent()), alfa);

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        // 1) Bloques fantasma (solo FALTA).
        boolean huboFantasma = false;
        int n = Math.min(entradas.size(), PaqueteGuia.MAX_ENTRADAS);
        for (int i = 0; i < n; i++) {
            PaqueteGuia.Entrada en = entradas.get(i);
            if (en.estado() != EstadoPos.FALTA) continue;
            BlockPos p = altar.offset(en.dx(), en.dy(), en.dz());
            BlockState estado = en.esperado() == Esperado.OBSIDIANA
                    ? Blocks.OBSIDIAN.defaultBlockState()
                    : Blocks.SOUL_TORCH.defaultBlockState();
            pose.pushPose();
            pose.translate(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            pose.scale(ESCALA, ESCALA, ESCALA);
            pose.translate(-0.5, -0.5, -0.5);
            bloques.renderSingleBlock(estado, pose, translucido, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
            pose.popPose();
            huboFantasma = true;
        }
        if (huboFantasma) buffers.endBatch(RenderType.translucent());

        // 2) Contornos: ámbar en FALTA, rojo en INCORRECTO.
        boolean huboLinea = false;
        VertexConsumer lineas = null;
        for (int i = 0; i < n; i++) {
            PaqueteGuia.Entrada en = entradas.get(i);
            if (en.estado() == EstadoPos.OK) continue;
            if (lineas == null) lineas = buffers.getBuffer(RenderType.lines());
            BlockPos p = altar.offset(en.dx(), en.dy(), en.dz());
            AABB caja = (en.estado() == EstadoPos.FALTA && en.esperado() == Esperado.FUEGO_ALMAS
                    ? CAJA_ANTORCHA : CAJA_BLOQUE)
                    .move(p).inflate(INFLAR);
            if (en.estado() == EstadoPos.FALTA) {
                LevelRenderer.renderLineBox(pose, lineas, caja, 1.0f, 0.7f, 0.2f, 0.9f);
            } else {
                LevelRenderer.renderLineBox(pose, lineas, caja, 1.0f, 0.2f, 0.2f, 1.0f);
            }
            huboLinea = true;
        }
        if (huboLinea) buffers.endBatch(RenderType.lines());

        pose.popPose();
    }

    /** Al salir del servidor la guía se borra (el servidor no avisa en una desconexión). */
    @SubscribeEvent
    public static void salir(ClientPlayerNetworkEvent.LoggingOut e) {
        GuiaCliente.limpiar();
    }

    /** Cambio de dimensión: el jugador se recrea; si la dimensión cambia, se borra la guía. */
    @SubscribeEvent
    public static void clonar(ClientPlayerNetworkEvent.Clone e) {
        if (e.getOldPlayer() == null || e.getNewPlayer() == null
                || e.getOldPlayer().level().dimension() != e.getNewPlayer().level().dimension()) {
            GuiaCliente.limpiar();
        }
    }

    /**
     * Envoltorio que multiplica el alfa de cada vértice. No sobrescribe {@code putBulkData}: los
     * métodos por defecto de {@link VertexConsumer} acaban llamando a {@link #vertex(float, float, float,
     * float, float, float, float, float, float, int, int, float, float, float)} de este envoltorio.
     */
    private static final class AlfaConsumer implements VertexConsumer {
        private final VertexConsumer base;
        private final float mult;

        AlfaConsumer(VertexConsumer base, float mult) {
            this.base = base;
            this.mult = mult;
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                           int overlay, int luz, float nx, float ny, float nz) {
            base.vertex(x, y, z, r, g, b, a * mult, u, v, overlay, luz, nx, ny, nz);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            base.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            base.color(r, g, b, Math.round(a * mult));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            base.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            base.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            base.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            base.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            base.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            base.defaultColor(r, g, b, Math.round(a * mult));
        }

        @Override
        public void unsetDefaultColor() {
            base.unsetDefaultColor();
        }
    }
}
