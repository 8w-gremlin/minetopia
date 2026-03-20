package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructure;
import com.minetopia.village.VillageStructureType;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * Miner walks to the mineshaft structure and produces ore on a timer.
 * Working hours: dayTime 500–11500.
 */
public class GoalMineAtShaft extends Goal {

    private static final int WORK_START       = 500;
    private static final int WORK_END         = 11500;
    private static final int STUCK_LIMIT      = 200;
    private static final int PRODUCTION_TICKS = 1200;
    /** Must be within this many blocks (squared) of shaft to produce. */
    private static final double NEAR_SHAFT_SQ = 256.0; // 16 blocks

    private final MinetopiaVillager villager;

    private BlockPos shaftPos       = null;
    private int stuckTimer          = 0;
    private int productionTimer     = 0;

    public GoalMineAtShaft(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        if (!hasPickaxe()) return false;
        if (villager.getVillageId().isEmpty()) return false;
        shaftPos = findShaftPos(serverLevel);
        return shaftPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (shaftPos == null) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return stuckTimer < STUCK_LIMIT;
    }

    @Override
    public void start() {
        stuckTimer = 0;
        productionTimer = 0;
        if (shaftPos != null) {
            villager.getNavigation().moveTo(
                    shaftPos.getX() + 0.5, shaftPos.getY(), shaftPos.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        shaftPos = null;
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (shaftPos == null) return;
        double distSq = villager.blockPosition().distSqr(shaftPos);
        if (distSq > NEAR_SHAFT_SQ) {
            // Still travelling
            stuckTimer++;
            return;
        }
        // Near shaft — produce on timer
        if (++productionTimer >= PRODUCTION_TICKS) {
            productionTimer = 0;
            produceOres();
        }
    }

    private void produceOres() {
        var inv = villager.getVillagerInventory();
        var rng = villager.getRandom();

        // 2-4 cobblestone
        inv.addItem(new ItemStack(Items.COBBLESTONE, 2 + rng.nextInt(3)));

        // 50% chance of 1 coal
        if (rng.nextFloat() < 0.5f) {
            inv.addItem(new ItemStack(Items.COAL, 1));
        }

        // 30% chance of 1 raw iron
        if (rng.nextFloat() < 0.3f) {
            inv.addItem(new ItemStack(Items.RAW_IRON, 1));
        }

        // 5% chance of diamond if carrying diamond pickaxe
        if (hasDiamondPickaxe() && rng.nextFloat() < 0.05f) {
            inv.addItem(new ItemStack(Items.DIAMOND, 1));
        }
    }

    private BlockPos findShaftPos(ServerLevel level) {
        return VillageManager.get(level)
                .findVillageById(villager.getVillageId().get())
                .flatMap(village -> village.getStructures().stream()
                        .filter(s -> s.type() == VillageStructureType.MINESHAFT)
                        .findFirst()
                        .map(VillageStructure::pos))
                .orElse(null);
    }

    private boolean hasPickaxe() {
        var inv = villager.getVillagerInventory();
        return ItemDesireSet.countItem(inv, Items.WOODEN_PICKAXE)    > 0
            || ItemDesireSet.countItem(inv, Items.STONE_PICKAXE)     > 0
            || ItemDesireSet.countItem(inv, Items.IRON_PICKAXE)      > 0
            || ItemDesireSet.countItem(inv, Items.GOLDEN_PICKAXE)    > 0
            || ItemDesireSet.countItem(inv, Items.DIAMOND_PICKAXE)   > 0
            || ItemDesireSet.countItem(inv, Items.NETHERITE_PICKAXE) > 0;
    }

    private boolean hasDiamondPickaxe() {
        return ItemDesireSet.countItem(villager.getVillagerInventory(), Items.DIAMOND_PICKAXE) > 0;
    }
}
