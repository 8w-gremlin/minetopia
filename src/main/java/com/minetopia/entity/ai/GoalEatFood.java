package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Villager eats a food item from their inventory when hunger drops below 20.
 */
public class GoalEatFood extends Goal {

    private static final int EAT_THRESHOLD = 20;
    private static final int EAT_DURATION  = 32; // ticks to finish eating

    private final MinetopiaVillager villager;
    private ItemStack eating = ItemStack.EMPTY;
    private int eatTimer;

    public GoalEatFood(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (villager.getHunger() >= EAT_THRESHOLD) return false;
        return !villager.getVillagerInventory().findFood().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return eatTimer < EAT_DURATION && !eating.isEmpty();
    }

    @Override
    public void start() {
        eating = villager.getVillagerInventory().findFood().copy();
        eatTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        eatTimer++;
        if (eatTimer >= EAT_DURATION) {
            applyFood();
        }
    }

    @Override
    public void stop() {
        eating = ItemStack.EMPTY;
        eatTimer = 0;
    }

    private void applyFood() {
        FoodProperties food = eating.get(DataComponents.FOOD);
        if (food != null) {
            int restored = food.nutrition() * 5; // scale nutrition to hunger points
            villager.setHunger(Math.min(100, villager.getHunger() + restored));
            villager.setHappiness(Math.min(100, villager.getHappiness() + 2));
        }
        // Remove one item from inventory
        ItemStack inInventory = villager.getVillagerInventory().findFood();
        if (!inInventory.isEmpty()) {
            inInventory.shrink(1);
        }
        // Visual feedback
        if (villager.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + villager.getBbHeight() * 0.8,
                    villager.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }
}
