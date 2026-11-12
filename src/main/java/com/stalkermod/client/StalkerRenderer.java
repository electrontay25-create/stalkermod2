package com.stalkermod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stalkermod.StalkerMod;
import com.stalkermod.entity.SpriteInfo;
import com.stalkermod.entity.StalkerEntity;
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
 * Renderer 2D: dibuja el PNG del Stalker como un cartel vertical que siempre mira a la camara
 * (solo gira en horizontal, nunca se inclina).
 *
 * MODO ACECHAR: el borde recto del PNG se pega al borde de la cobertura (arbol/pared). Al esconderse,
 *   el sprite se desliza hacia atras de la cobertura y se desvanece.
 *   En la forma B el brazo levantado se dibuja un poco mas adelante, "agarrado" a la cobertura.
 * MODO PUERTA: cara en la ventana; despues el sprite B (brazo levantado) golpea hacia la puerta.
 */
public class StalkerRenderer extends EntityRenderer<StalkerEntity> {

    private static final ResourceLocation TEX_A = new ResourceLocation(StalkerMod.MODID, "textures/entity/stalker_a.png");
    private static final ResourceLocation TEX_B = new ResourceLocation(StalkerMod.MODID, "textures/entity/stalker_b.png");
    private static final ResourceLocation TEX_HEAD = new ResourceLocation(StalkerMod.MODID, "textures/entity/stalker_head.png");

    private static final float DEG = (float) Math.PI / 180.0F;
    /** Cuanto se adelanta el brazo de la forma B para quedar por delante del tronco/pared. */
    private static final float ARM_FORWARD = 1.14F;

    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return textureFor(entity.getSprite());
    }

    private static ResourceLocation textureFor(int sprite) {
        switch (sprite) {
            case StalkerEntity.SPRITE_B:
                return TEX_B;
            case StalkerEntity.SPRITE_HEAD:
                return TEX_HEAD;
            default:
                return TEX_A;
        }
    }

    @Override
    public void render(StalkerEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        float reveal = Mth.lerp(partialTick, entity.getPrevReveal(), entity.getReveal());
        if (reveal <= 0.003F) {
            return;
        }
        int sprite = entity.getSprite();
        SpriteInfo info = SpriteInfo.get(sprite);

        // Orientacion de la camara (horizontal).
        float camYaw = this.entityRenderDispatcher.camera.getYRot();
        float camRad = camYaw * DEG;
        float rx = -Mth.cos(camRad);   // vector "derecha de la pantalla" en el mundo
        float rz = -Mth.sin(camRad);

        // Lado hacia el que sobresale (acechar) / donde esta la puerta (puerta), en el mundo.
        float by = entity.getBodyYawData() * DEG;
        float sx = entity.getLean() * Mth.cos(by);
        float sz = entity.getLean() * Mth.sin(by);
        boolean sideRight = (sx * rx + sz * rz) >= 0.0F;   // ¿ese lado cae a la derecha de la pantalla?

        // Un minimo de luz para que se distinga de noche.
        int blockLight = Math.max(LightTexture.block(light), 8);
        int packed = LightTexture.pack(blockLight, LightTexture.sky(light));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - camYaw));   // cartel de cara a la camara
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(textureFor(sprite)));

        if (entity.getMode() == StalkerEntity.MODE_STALK) {
            renderStalk(vc, ps, info, sprite, reveal, sideRight, packed);
        } else if (entity.getMode() == StalkerEntity.MODE_WATCH) {
            renderWatch(vc, ps, entity, info, reveal, packed, partialTick);
        } else {
            renderDoor(vc, ps, entity, info, sprite, reveal, sideRight, packed, partialTick);
        }
        ps.popPose();
    }

    // ---------------------------------------------------------------------
    //  Acechar
    // ---------------------------------------------------------------------

    private void renderStalk(VertexConsumer vc, PoseStack ps, SpriteInfo info, int sprite,
                             float reveal, boolean sideRight, int light) {
        float w = info.width;
        float h = info.height;
        float slideMax = Math.min(w, 1.0F);
        float shift = (1.0F - reveal) * slideMax;            // cuanto se ha metido detras de la cobertura
        int alpha = (int) (Mth.clamp(reveal * 2.0F, 0.0F, 1.0F) * 255.0F);
        float anchorX = sideRight ? -shift : shift;          // posicion local del borde de la cobertura
        PoseStack.Pose pose = ps.last();

        if (sprite == StalkerEntity.SPRITE_B) {
            float a = info.anchor;
            if (sideRight) {
                // brazo (izquierda del borde), adelantado
                quad(vc, pose, anchorX - a * w, anchorX, 0.0F, h, 0.0F, a, ARM_FORWARD, light, alpha);
                // cuerpo + cabeza (derecha del borde)
                quad(vc, pose, anchorX, anchorX + (1.0F - a) * w, 0.0F, h, a, 1.0F, 0.0F, light, alpha);
            } else {
                quad(vc, pose, anchorX, anchorX + a * w, 0.0F, h, a, 0.0F, ARM_FORWARD, light, alpha);
                quad(vc, pose, anchorX - (1.0F - a) * w, anchorX, 0.0F, h, 1.0F, a, 0.0F, light, alpha);
            }
        } else {
            if (sideRight) {
                quad(vc, pose, anchorX, anchorX + w, 0.0F, h, 0.0F, 1.0F, 0.0F, light, alpha);
            } else {
                quad(vc, pose, anchorX - w, anchorX, 0.0F, h, 1.0F, 0.0F, 0.0F, light, alpha);
            }
        }
    }

    // ---------------------------------------------------------------------
    //  Media cabeza: la cara esta centrada en su posicion; el muro del mundo tapa la otra mitad.
    //  Al desaparecer se desvanece lentamente.
    // ---------------------------------------------------------------------

    private void renderWatch(VertexConsumer vc, PoseStack ps, StalkerEntity entity, SpriteInfo info,
                             float reveal, int light, float partialTick) {
        float t = entity.tickCount + partialTick;
        int alpha = (int) (Mth.clamp(reveal, 0.0F, 1.0F) * 255.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.05F) * 3.0F));   // balanceo lento e inquietante
        quad(vc, ps.last(), -info.width * 0.5F, info.width * 0.5F, -info.height * 0.5F, info.height * 0.5F,
                0.0F, 1.0F, 0.0F, light, alpha);
    }

    // ---------------------------------------------------------------------
    //  Puerta (el sprite esta centrado en la cabeza = centro de la ventana)
    // ---------------------------------------------------------------------

    private void renderDoor(VertexConsumer vc, PoseStack ps, StalkerEntity entity, SpriteInfo info, int sprite,
                            float reveal, boolean doorOnRight, int light, float partialTick) {
        float t = entity.tickCount + partialTick;
        float scale = (sprite == StalkerEntity.SPRITE_B) ? 1.24F : 1.0F;
        float w = info.width * scale;
        float h = info.height * scale;
        int alpha = (int) (Mth.clamp(reveal, 0.0F, 1.0F) * 255.0F);

        // Movimiento segun la fase (el sprite gira alrededor del centro de la cabeza).
        float tilt = 0.0F;
        float shakeX = 0.0F;
        switch (entity.getPoseState()) {
            case StalkerEntity.POSE_WINDOW:
                tilt = Mth.sin(t * 0.09F) * 2.5F;
                shakeX = Mth.sin(t * 1.3F) * 0.004F;
                break;
            case StalkerEntity.POSE_REACH:
                tilt = Mth.sin(t * 0.5F) * 4.0F;
                shakeX = Mth.sin(t * 0.9F) * 0.012F;
                break;
            case StalkerEntity.POSE_POUND:
                float hit = Mth.sin(t * 1.7F);
                tilt = hit * 7.0F;
                shakeX = hit * 0.035F;
                break;
            default:
                tilt = Mth.sin(t * 0.3F) * 5.0F;
                break;
        }
        ps.translate(shakeX, 0.0F, 0.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(tilt));
        PoseStack.Pose pose = ps.last();

        float hy = info.headFy;
        float y0 = -(1.0F - hy) * h;
        float y1 = hy * h;

        if (sprite == StalkerEntity.SPRITE_HEAD) {
            quad(vc, pose, -w * 0.5F, w * 0.5F, -h * 0.5F, h * 0.5F, 0.0F, 1.0F, 0.0F, light, alpha);
            return;
        }
        // Forma B: el brazo levantado va del lado de la puerta.
        float hx = info.headFx;
        if (!doorOnRight) {
            quad(vc, pose, -hx * w, (1.0F - hx) * w, y0, y1, 0.0F, 1.0F, 0.0F, light, alpha);
        } else {
            quad(vc, pose, -(1.0F - hx) * w, hx * w, y0, y1, 1.0F, 0.0F, 0.0F, light, alpha);
        }
    }

    // ---------------------------------------------------------------------

    /**
     * Dibuja un rectangulo vertical en el plano local (visible desde +Z).
     * uL/uR = coordenada U en el borde izquierdo/derecho (se pueden invertir para espejar).
     */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float x1, float y0, float y1,
                             float uL, float uR, float z, int light, int alpha) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vc.vertex(m, x0, y0, z).color(255, 255, 255, alpha).uv(uL, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x1, y0, z).color(255, 255, 255, alpha).uv(uR, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x1, y1, z).color(255, 255, 255, alpha).uv(uR, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(m, x0, y1, z).color(255, 255, 255, alpha).uv(uL, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
