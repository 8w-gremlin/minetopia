package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * Blacksmith passively converts iron ingots into random iron tools on a timer.
 * Runs every 800 ticks: consumes 2 iron ingots, produces 1 random iron tool.
 */
public class GoalSmithItems extends Goal {

    private static final int SMITH_INTERVAL = 800;
    private static final Item[] IRON_TOOLS = {
            Items.IRON_SWORD,
            Items.IRON_PICKAXE,
            Items.IRON_AXE,
            Items.IRON_SHOVEL
    };

    private final MinetopiaVillager villager;
    private int workTimer = 0;

    public GoalSmithItems(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return !(villager.level().isClientSide());
    }

    @Override
    public boolean canContinueToUse() {
        return false; // single-tick re-evaluation
    }

    @Override
    public void tick() {
        if (++workTimer < SMITH_INTERVAL) return;
        workTimer = 0;

        var inv = villager.getVillagerInventory();
        if (ItemDesireSet.countItem(inv, Items.IRON_INGOT) < 2) return;

        // Remove 2 iron ingots
        int toRemove = 2;
        for (int i = 0; i < inv.getContainerSize() && toRemove > 0; i++) {
            var stack = inv.getItem(i);
            if (!stack.is(Items.IRON_INGOT)) continue;
            int remove = Math.min(stack.getCount(), toRemove);
            inv.removeItem(i, remove);
            toRemove -= remove;
        }

        // Add 1 random iron tool
        Item tool = IRON_TOOLS[villager.getRandom().nextInt(IRON_TOOLS.length)];
        inv.addItem(new ItemStack(tool, 1));
    }
}
