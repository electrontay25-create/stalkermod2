package com.stalkermod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stalkermod.StalkerMod;
import com.stalkermod.entity.AnderdingusEntity;
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
 * Anderdingus 2D (cartel que siempre mira a la camara). Todos los cuadros estan en una sola textura (5 x 4):
 *   0      = dormido (pose de pie)
 *   1..12  = animacion de despertar (fundidos entre poses, segun WAKE_TIMES)
 *   13..16 = forma roja furiosa (ciclo de 4 cuadros a 30 por segundo) mientras te persigue
 */
public class AnderdingusRenderer extends EntityRenderer<AnderdingusEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(StalkerMod.MODID, "textures/entity/anderdingus.png");

    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int CELL_W = 288;
    private static final int CELL_H = 290;

    /** Tamano del cartel en bloques. */
    private static final float WIDTH = 3.3F;
    private static final float HEIGHT = WIDTH * CELL_H / CELL_W;

    /** Segundo (desde que empieza a despertar) en que aparece cada cuadro 0..12 de la animacion de despertar. */
    private static final float[] WAKE_TIMES = {
            0.00F, 0.03F, 0.10F, 0.13F, 0.17F, 0.53F, 0.57F, 0.60F, 0.63F, 0.93F, 1.00F, 1.03F, 1.83F
    };
    private static final int RED_FIRST = 13;

    public AnderdingusRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AnderdingusEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AnderdingusEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        int state = entity.getState();
        float sec = (entity.getStateTicks() + partialTick) / 20.0F;

        int cell;
        if (state == AnderdingusEntity.STATE_SLEEP) {
            cell = 0;
        } else if (state == AnderdingusEntity.STATE_WAKE) {
            cell = 0;
            for (int i = 0; i < WAKE_TIMES.length; i++) {
                if (sec >= WAKE_TIMES[i]) {
                    cell = i;
                }
            }
        } else {
            cell = RED_FIRST + ((int) (sec * 30.0F)) % 4;
        }

        int col = cell % COLS;
        int row = cell / COLS;
        float du = 0.5F / (CELL_W * COLS);
        float dv = 0.5F / (CELL_H * ROWS);
        float u0 = col / (float) COLS + du;
        float u1 = (col + 1) / (float) COLS - du;
        float v0 = row / (float) ROWS + dv;
        float v1 = (row + 1) / (float) ROWS - dv;

        int blockLight = Math.max(LightTexture.block(light), 11);
        int packed = LightTexture.pack(blockLight, LightTexture.sky(light));

        float t = entity.tickCount + partialTick;
        float bob = 0.0F;
        float tilt = 0.0F;
        if (state == AnderdingusEntity.STATE_CHASE) {
            bob = 0.07F * Math.abs(Mth.sin(t * 0.9F));      // rebote al correr
            tilt = 2.5F * Mth.sin(t * 0.45F);               // balanceo
        } else if (state == AnderdingusEntity.STATE_SLEEP) {
            tilt = 0.8F * Mth.sin(t * 0.05F);               // respiracion lenta
        }

        float camYaw = this.entityRenderDispatcher.camera.getYRot();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - camYaw));   // siempre de cara a la camara
        ps.translate(0.0F, bob, 0.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(tilt));               // gira alrededor de los pies
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        quad(vc, ps.last(), -WIDTH * 0.5F, WIDTH * 0.5F, 0.0F, HEIGHT, u0, u1, v0, v1, packed);
        ps.popPose();
    }

    /** Rectangulo vertical en el plano local, visible desde +Z. v0 = borde superior, v1 = borde inferior. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float x1, float y0, float y1,
                             float u0, float u1, float v0, float v1, int light) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vc.vertex(m, x0, y0, 0.0F).color(255, 255, 255, 255).uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x1, y0, 0.0F).color(255, 255, 255, 255).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x1, y1, 0.0F).color(255, 255, 255, 255).uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x0, y1, 0.0F).color(255, 255, 255, 255).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
