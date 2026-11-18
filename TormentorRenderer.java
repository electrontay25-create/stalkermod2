package com.stalkermod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stalkermod.StalkerMod;
import com.stalkermod.entity.TormentorEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Reproduce la animacion del Tormentor (108 cuadros del video, 12 por segundo) como un cartel plano pegado a la ventana,
 * paralelo al muro, centrado en el bloque de la ventana. Los cuadros estan en 3 texturas (atlas de 6x6).
 */
public class TormentorRenderer extends EntityRenderer<TormentorEntity> {

    private static final ResourceLocation[] ATLAS = {
            new ResourceLocation(StalkerMod.MODID, "textures/entity/tormentor_0.png"),
            new ResourceLocation(StalkerMod.MODID, "textures/entity/tormentor_1.png"),
            new ResourceLocation(StalkerMod.MODID, "textures/entity/tormentor_2.png")
    };
    private static final int COLS = 6;
    private static final int ROWS = 6;
    private static final int PER_ATLAS = COLS * ROWS;
    private static final int CELL_W = 288;
    private static final int CELL_H = 238;

    /** Tamano del cartel en bloques (ancho); el alto sale de la proporcion del video. */
    private static final float WIDTH = 2.7F;
    private static final float HEIGHT = WIDTH * CELL_H / CELL_W;

    public TormentorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TormentorEntity entity) {
        return ATLAS[0];
    }

    @Override
    public void render(TormentorEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        float reveal = Mth.lerp(partialTick, entity.getPrevReveal(), entity.getReveal());
        if (reveal <= 0.003F) {
            return;
        }

        // Cuadro de la animacion segun el tiempo de vida (tickCount) -> 12 cuadros por segundo.
        float t = entity.tickCount + partialTick;
        int frame = Mth.clamp((int) (t * (TormentorEntity.FPS / 20.0F)), 0, TormentorEntity.FRAME_COUNT - 1);
        int atlasIndex = Math.min(frame / PER_ATLAS, ATLAS.length - 1);
        int cell = frame % PER_ATLAS;
        int col = cell % COLS;
        int row = cell / COLS;

        // Media "esquina" de margen para que no se cuelen cuadros vecinos.
        float du = 0.5F / (CELL_W * COLS);
        float dv = 0.5F / (CELL_H * ROWS);
        float u0 = col / (float) COLS + du;
        float u1 = (col + 1) / (float) COLS - du;
        float v0 = row / (float) ROWS + dv;
        float v1 = (row + 1) / (float) ROWS - dv;

        int blockLight = Math.max(LightTexture.block(light), 12);
        int packed = LightTexture.pack(blockLight, LightTexture.sky(light));
        int alpha = (int) (Mth.clamp(reveal, 0.0F, 1.0F) * 255.0F);

        ps.pushPose();
        // Cartel paralelo al muro, mirando hacia adentro de la casa (yaw = direccion "hacia adentro").
        ps.mulPose(Axis.YP.rotationDegrees(-entity.getBodyYawData()));
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(ATLAS[atlasIndex]));
        quad(vc, ps.last(), -WIDTH * 0.5F, WIDTH * 0.5F, -HEIGHT * 0.5F, HEIGHT * 0.5F, u0, u1, v0, v1, packed, alpha);
        ps.popPose();
    }

    /** Rectangulo vertical en el plano local, visible desde +Z. v0 = borde superior, v1 = borde inferior. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float x1, float y0, float y1,
                             float u0, float u1, float v0, float v1, int light, int alpha) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vc.vertex(m, x0, y0, 0.0F).color(255, 255, 255, alpha).uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x1, y0, 0.0F).color(255, 255, 255, alpha).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x1, y1, 0.0F).color(255, 255, 255, alpha).uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x0, y1, 0.0F).color(255, 255, 255, alpha).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
