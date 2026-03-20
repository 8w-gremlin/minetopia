package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.Map;

/**
 * Chef passively converts raw food into cooked food on a timer.
 * Runs every 600 ticks: consumes 1 raw food, produces 1 cooked food.
 */
public class GoalCookFood extends Goal {

    private static final int COOK_INTERVAL = 600;

    private static final Map<Item, Item> COOK_MAP = Map.of(
            Items.BEEF,        Items.COOKED_BEEF,
            Items.PORKCHOP,    Items.COOKED_PORKCHOP,
            Items.CHICKEN,     Items.COOKED_CHICKEN,
            Items.MUTTON,      Items.COOKED_MUTTON,
            Items.COD,         Items.COOKED_COD,
            Items.SALMON,      Items.COOKED_SALMON,
            Items.POTATO,      Items.BAKED_POTATO
    );

    private final MinetopiaVillager villager;
    private int workTimer = 0;

    public GoalCookFood(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return !(villager.level().isClientSide());
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void tick() {
        if (++workTimer < COOK_INTERVAL) return;
        workTimer = 0;

        var inv = villager.getVillagerInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            Item cooked = COOK_MAP.get(stack.getItem());
            if (cooked == null) continue;
            // Remove 1 raw, add 1 cooked
            inv.removeItem(i, 1);
            inv.addItem(new ItemStack(cooked, 1));
            return; // only one conversion per interval
        }
    }
}
