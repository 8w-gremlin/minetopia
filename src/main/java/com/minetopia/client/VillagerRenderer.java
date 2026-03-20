package com.minetopia.client;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerRenderer<T extends MinetopiaVillager>
        extends HumanoidMobRenderer<T, HumanoidModel<T>> {

    private final ResourceLocation maleTex;
    private final ResourceLocation femaleTex;

    @SuppressWarnings("unchecked")
    public VillagerRenderer(EntityRendererProvider.Context ctx,
                            String textureName, boolean hasGenderModel) {
        super(ctx,
              (HumanoidModel<T>)(HumanoidModel<?>) new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)),
              0.5f);
        this.maleTex = ResourceLocation.fromNamespaceAndPath("minetopia",
                "textures/entity/" + textureName + "_m.png");
        this.femaleTex = hasGenderModel
                ? ResourceLocation.fromNamespaceAndPath("minetopia",
                        "textures/entity/" + textureName + "_f.png")
                : this.maleTex;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.isMale() ? maleTex : femaleTex;
    }
}
