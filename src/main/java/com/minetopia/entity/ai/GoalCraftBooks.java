package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * During working hours, craft books from paper and leather in the villager's inventory.
 * Recipe: 3 paper + 1 leather → 1 book (vanilla recipe).
 * Fires in-place with no movement.
 */
public class GoalCraftBooks extends Goal {

    private static final int WORK_START    = 500;
    private static final int WORK_END      = 11500;
    private static final int CRAFT_INTERVAL = 40; // craft every 2 seconds

    private final MinetopiaVillager villager;
    private int craftTimer = 0;

    public GoalCraftBooks(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        long dayTime = villager.level().getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return countItem(Items.PAPER) >= 3 && countItem(Items.LEATHER) >= 1;
    }

    @Override
    public boolean canContinueToUse() {
        long dayTime = villager.level().getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return countItem(Items.PAPER) >= 3 && countItem(Items.LEATHER) >= 1;
    }

    @Override
    public void tick() {
        if (++craftTimer < CRAFT_INTERVAL) return;
        craftTimer = 0;
        craftBooks();
    }

    @Override
    public void stop() {
        craftTimer = 0;
    }

    private void craftBooks() {
        var inv = villager.getVillagerInventory();
        int paper   = countItem(Items.PAPER);
        int leather = countItem(Items.LEATHER);
        // Each book costs 3 paper + 1 leather
        int batches = Math.min(paper / 3, leather);
        if (batches <= 0) return;

        consume(Items.PAPER,   batches * 3);
        consume(Items.LEATHER, batches);

        ItemStack books = new ItemStack(Items.BOOK, batches);
        ItemStack leftover = inv.addItem(books);
        if (!leftover.isEmpty()) {
            villager.level().addFreshEntity(new ItemEntity(
                    villager.level(),
                    villager.getX(), villager.getY(), villager.getZ(),
                    leftover));
        }
    }

    private int countItem(net.minecraft.world.item.Item item) {
        var inv = villager.getVillagerInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) total += inv.getItem(i).getCount();
        }
        return total;
    }

    private void consume(net.minecraft.world.item.Item item, int amount) {
        var inv = villager.getVillagerInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (!slot.is(item)) continue;
            int take = Math.min(slot.getCount(), remaining);
            slot.shrink(take);
            inv.setItem(i, slot);
            remaining -= take;
        }
    }
}
