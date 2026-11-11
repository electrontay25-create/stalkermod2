package com.stalkermod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stalkermod.StalkerMod;
import com.stalkermod.entity.StalkerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class StalkerRenderer extends MobRenderer<StalkerEntity, StalkerModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(StalkerMod.MODID, "textures/entity/stalker.png");

    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new StalkerModel(context.bakeLayer(StalkerModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return TEXTURE;
    }

    /** El modelo esta dibujado a escala x8, lo reducimos a 1/8. (Los pies se mantienen en el suelo.) */
    @Override
    protected void scale(StalkerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.125F, 0.125F, 0.125F);
    }

    /** Render translucido: necesario para el fade-in / fade-out. */
    @Override
    protected RenderType getRenderType(StalkerEntity entity, boolean visible, boolean visibleToPlayer, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    @Override
    protected boolean shouldShowName(StalkerEntity entity) {
        return false;
    }

    /** Un minimo de luz para que la cara se distinga de noche aunque este en un lugar oscuro. */
    @Override
    protected int getBlockLightLevel(StalkerEntity entity, BlockPos pos) {
        return Math.max(super.getBlockLightLevel(entity, pos), 8);
    }
}
