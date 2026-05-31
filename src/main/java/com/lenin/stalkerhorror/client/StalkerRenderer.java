package com.lenin.stalkerhorror.client;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StalkerRenderer extends MobRenderer<StalkerEntity, HumanoidModel<StalkerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");

    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(StalkerEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.7F, 1.3F, 0.7F);
        super.scale(entity, poseStack, partialTickTime);
    }
}