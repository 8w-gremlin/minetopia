package com.minetopia.client;

import com.minetopia.entity.VillagerChild;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChildVillagerRenderer extends VillagerRenderer<VillagerChild> {

    public ChildVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "child", true);
    }

    @Override
    protected void scale(VillagerChild entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(0.5f, 0.5f, 0.5f);
    }
}
