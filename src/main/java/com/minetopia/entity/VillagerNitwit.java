package com.minetopia.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** No profession — wanders, eats, and sleeps. */
public class VillagerNitwit extends MinetopiaVillager {

    public VillagerNitwit(EntityType<? extends VillagerNitwit> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    protected String getTradeName() { return "Nitwit"; }
}
