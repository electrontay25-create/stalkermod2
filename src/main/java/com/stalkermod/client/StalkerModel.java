package com.stalkermod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stalkermod.StalkerMod;
import com.stalkermod.entity.StalkerEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo del Stalker: figura altisima y delgada, cabeza palida, brazos larguisimos con dedos.
 *
 * El modelo esta dibujado a ESCALA x8 (cada "pixel" mide 1/128 de bloque) para poder usar una
 * textura de 512x512 con la cara en buena calidad. El StalkerRenderer lo reduce con scale(0.125).
 * Altura total: 368 / 128 = 2.875 bloques.
 *
 * Todas las partes cuelgan de "pivot", que esta en los pies; asi se puede inclinar todo el cuerpo.
 */
public class StalkerModel extends EntityModel<StalkerEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(StalkerMod.MODID, "stalker"), "main");

    private static final float DEG = (float) Math.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart pivot;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart[] leftFingers = new ModelPart[4];
    private final ModelPart[] rightFingers = new ModelPart[4];
    private float alpha = 1.0F;

    public StalkerModel(ModelPart root) {
        this.root = root;
        this.pivot = root.getChild("pivot");
        this.head = pivot.getChild("head");
        this.leftArm = pivot.getChild("left_arm");
        this.rightArm = pivot.getChild("right_arm");
        for (int i = 0; i < 4; i++) {
            this.leftFingers[i] = leftArm.getChild("finger" + i);
            this.rightFingers[i] = rightArm.getChild("finger" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Los pies estan en y = 24 (convencion de Minecraft); todo lo demas es relativo a los pies.
        PartDefinition pivot = root.addOrReplaceChild("pivot", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Torso + cuello (negro)
        pivot.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(60, 128).addBox(-14.0F, -112.0F, -6.0F, 28.0F, 112.0F, 12.0F)
                        .texOffs(150, 128).addBox(-5.0F, -132.0F, -5.0F, 10.0F, 20.0F, 10.0F),
                PartPose.offset(0.0F, -176.0F, 0.0F));

        // Cabeza: la cara esta en la textura (0,0). Pivote en la base del cuello.
        pivot.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-26.0F, -60.0F, -26.0F, 52.0F, 60.0F, 52.0F),
                PartPose.offset(0.0F, -308.0F, 0.0F));

        // Piernas delgadas
        pivot.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 128).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 176.0F, 10.0F),
                PartPose.offset(7.0F, -176.0F, 0.0F));
        pivot.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 128).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 176.0F, 10.0F),
                PartPose.offset(-7.0F, -176.0F, 0.0F));

        // Brazos larguisimos, con 4 dedos finos cada uno
        PartDefinition leftArm = pivot.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 128).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 184.0F, 10.0F),
                PartPose.offset(19.0F, -284.0F, 0.0F));
        PartDefinition rightArm = pivot.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 128).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 184.0F, 10.0F),
                PartPose.offset(-19.0F, -284.0F, 0.0F));
        for (int i = 0; i < 4; i++) {
            float fx = -3.0F + i * 2.0F;
            float len = (i == 1 || i == 2) ? 56.0F : 48.0F;
            CubeListBuilder finger = CubeListBuilder.create()
                    .texOffs(200, 128).addBox(-1.0F, 0.0F, -1.0F, 2.0F, len, 2.0F);
            leftArm.addOrReplaceChild("finger" + i, finger, PartPose.offset(fx, 184.0F, 0.0F));
            rightArm.addOrReplaceChild("finger" + i, finger, PartPose.offset(fx, 184.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 512, 512);
    }

    // ---------------------------------------------------------------------

    @Override
    public void prepareMobModel(StalkerEntity e, float limbSwing, float limbSwingAmount, float partialTick) {
        this.alpha = Mth.lerp(partialTick, e.getPrevAlpha(), e.getAlpha());
    }

    /**
     * Animaciones por codigo (sin archivos de animacion). Convenciones:
     *  - zRot positivo en pivot/cabeza = se inclina hacia la IZQUIERDA del Stalker.
     *  - Brazo derecho: zRot positivo lo separa del cuerpo. Brazo izquierdo: zRot negativo.
     *  - Con el brazo en horizontal, xRot lo mueve arriba/abajo (movimiento de golpear).
     */
    @Override
    public void setupAnim(StalkerEntity e, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        float t = age;
        int pose = e.getPoseState();
        float lean = e.getLean();
        float side = e.isArmLeft() ? 1.0F : -1.0F;       // +1 = el brazo activo es el izquierdo
        float breathe = Mth.sin(t * 0.05F);

        // --- base: cabeza sigue al jugador ---
        pivot.xRot = 0.0F;
        pivot.yRot = 0.0F;
        pivot.zRot = 0.0F;
        head.yRot = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -75.0F, 75.0F) * DEG;
        head.xRot = Mth.clamp(headPitch, -35.0F, 35.0F) * DEG;
        head.zRot = 0.0F;

        // brazos colgando por defecto
        leftArm.xRot = 0.06F;
        rightArm.xRot = 0.06F;
        leftArm.yRot = 0.0F;
        rightArm.yRot = 0.0F;
        leftArm.zRot = -(0.07F + 0.03F * breathe);
        rightArm.zRot = 0.07F + 0.03F * breathe;

        float fingerSpeed = 0.35F;
        float fingerCurl = 0.25F;

        switch (pose) {
            case StalkerEntity.POSE_PEEK -> {
                // Asomado: cuerpo y cabeza inclinados hacia el lado por el que se asoma.
                pivot.zRot = lean * (0.10F + 0.015F * breathe);
                head.zRot = lean * (0.28F + 0.04F * breathe);
            }
            case StalkerEntity.POSE_WINDOW -> {
                // Cara pegada al vidrio, con pequenos tics.
                head.zRot = 0.10F + Mth.sin(t * 0.09F) * 0.07F;
                head.xRot += Mth.sin(t * 0.13F) * 0.04F;
            }
            case StalkerEntity.POSE_REACH -> {
                // Un brazo se estira de lado hacia la puerta y mueve la manija.
                ModelPart arm = e.isArmLeft() ? leftArm : rightArm;
                float mag = 1.45F + Mth.sin(t * 0.4F) * 0.08F;
                arm.zRot = -side * mag;
                arm.xRot = Mth.sin(t * 0.9F) * 0.22F;
                pivot.zRot = side * 0.10F;
                head.zRot = side * 0.12F + Mth.sin(t * 0.25F) * 0.05F;
                fingerSpeed = 0.7F;
                fingerCurl = 0.35F;
            }
            case StalkerEntity.POSE_POUND -> {
                // Golpes fuertes: el brazo sube y baja rapido, el cuerpo se sacude.
                ModelPart arm = e.isArmLeft() ? leftArm : rightArm;
                float hit = Mth.sin(t * 1.7F);
                arm.zRot = -side * 1.45F;
                arm.xRot = hit * 0.6F;
                pivot.zRot = side * 0.12F;
                pivot.xRot = hit * 0.035F;
                head.zRot = side * 0.15F + hit * 0.06F;
                fingerSpeed = 1.1F;
                fingerCurl = 0.5F;
            }
            default -> {
                // POSE_BROKE: ya rompio la puerta, estira el brazo hacia adentro, dedos temblando.
                ModelPart arm = e.isArmLeft() ? leftArm : rightArm;
                arm.zRot = -side * 1.30F;
                arm.xRot = -0.35F + Mth.sin(t * 0.3F) * 0.2F;
                pivot.zRot = side * 0.14F;
                head.zRot = side * 0.22F + Mth.sin(t * 0.2F) * 0.05F;
                fingerSpeed = 0.9F;
                fingerCurl = 0.45F;
            }
        }

        // Dedos: se abren en abanico y se mueven (movimiento inquietante).
        for (int i = 0; i < 4; i++) {
            float wiggle = 0.5F + 0.5F * Mth.sin(t * fingerSpeed + i * 1.7F);
            float spread = (i - 1.5F) * 0.17F;
            leftFingers[i].xRot = 0.10F + fingerCurl * wiggle;
            leftFingers[i].zRot = spread;
            rightFingers[i].xRot = 0.10F + fingerCurl * (1.0F - wiggle);
            rightFingers[i].zRot = spread;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alphaIn) {
        // Multiplicamos por nuestro alpha para el fade-in / fade-out.
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alphaIn * this.alpha);
    }
}
