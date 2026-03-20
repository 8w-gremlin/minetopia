package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * During working hours, convert sugar cane from the villager's inventory into paper.
 * 3 sugar cane → 3 paper (1:1 ratio, matching the vanilla recipe).
 * Fires in-place with no movement — crafting happens at the villager's position.
 */
public class GoalMakePaper extends Goal {

    private static final int WORK_START = 500;
    private static final int WORK_END   = 11500;
    private static final int CRAFT_INTERVAL = 40; // craft every 2 seconds

    private final MinetopiaVillager villager;
    private int craftTimer = 0;

    public GoalMakePaper(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.noneOf(Flag.class)); // no movement lock — runs alongside other goals
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        long dayTime = villager.level().getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return countSugarCane() >= 3;
    }

    @Override
    public boolean canContinueToUse() {
        long dayTime = villager.level().getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return countSugarCane() >= 3;
    }

    @Override
    public void tick() {
        if (++craftTimer < CRAFT_INTERVAL) return;
        craftTimer = 0;
        craftPaper();
    }

    @Override
    public void stop() {
        craftTimer = 0;
    }

    private void craftPaper() {
        var inv = villager.getVillagerInventory();
        int toConsume = 0;
        // Count available sugar cane in batches of 3
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.is(Items.SUGAR_CANE)) toConsume += slot.getCount();
        }
        toConsume = (toConsume / 3) * 3; // round down to multiple of 3
        if (toConsume <= 0) return;

        // Consume sugar cane
        int remaining = toConsume;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (!slot.is(Items.SUGAR_CANE)) continue;
            int take = Math.min(slot.getCount(), remaining);
            slot.shrink(take);
            inv.setItem(i, slot);
            remaining -= take;
        }

        // Produce paper (1:1 ratio — 3 cane → 3 paper)
        int paperProduced = toConsume;
        ItemStack paper = new ItemStack(Items.PAPER, paperProduced);
        ItemStack leftover = inv.addItem(paper);
        // If inventory is full, leftover is dropped at villager's feet
        if (!leftover.isEmpty()) {
            villager.level().addFreshEntity(
                    new net.minecraft.world.entity.item.ItemEntity(
                            villager.level(),
                            villager.getX(), villager.getY(), villager.getZ(),
                            leftover));
        }
    }

    private int countSugarCane() {
        var inv = villager.getVillagerInventory();
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(Items.SUGAR_CANE)) count += inv.getItem(i).getCount();
        }
        return count;
    }
}
