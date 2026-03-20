package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Farmer plants seeds on bare farmland and harvests mature crops.
 * Working hours: dayTime 500–11500.
 */
public class GoalFarmCrops extends Goal {

    private static final double REACH_DIST_SQ = 6.25; // 2.5 blocks
    private static final int WORK_START = 500;
    private static final int WORK_END   = 11500;
    private static final int STUCK_LIMIT = 80;

    private final MinetopiaVillager villager;

    /** The farmland position we are targeting (crop is at targetPos.above()). */
    private BlockPos targetPos = null;
    private boolean isHarvest  = false;
    private int stuckTimer     = 0;

    public GoalFarmCrops(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return findTarget(serverLevel);
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        if (!(villager.level() instanceof ServerLevel)) return false;
        long dayTime = ((ServerLevel) villager.level()).getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return stuckTimer < STUCK_LIMIT;
    }

    @Override
    public void start() {
        stuckTimer = 0;
        if (targetPos != null) {
            villager.getNavigation().moveTo(
                    targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        targetPos = null;
        isHarvest = false;
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos == null) return;
        if (villager.blockPosition().distSqr(targetPos) <= REACH_DIST_SQ) {
            ServerLevel level = (ServerLevel) villager.level();
            if (isHarvest) {
                doHarvest(level);
            } else {
                doPlant(level);
            }
            stop();
        } else {
            stuckTimer++;
        }
    }

    private void doHarvest(ServerLevel level) {
        BlockPos cropPos = targetPos.above();
        BlockState state = level.getBlockState(cropPos);
        Block block = state.getBlock();

        // Remove the crop block (drop = false; we hand-craft drops)
        level.destroyBlock(cropPos, false);

        // Hand-craft drops
        var rng = villager.getRandom();
        if (block == Blocks.WHEAT) {
            giveItem(Items.WHEAT, 1);
            giveItem(Items.WHEAT_SEEDS, rng.nextInt(3));
            // Replant if we have seeds
            if (hasSeed(Items.WHEAT_SEEDS)) {
                level.setBlock(cropPos, Blocks.WHEAT.defaultBlockState(), 3);
                removeSeed(Items.WHEAT_SEEDS);
            }
        } else if (block == Blocks.CARROTS) {
            giveItem(Items.CARROT, 2 + rng.nextInt(3));
            if (hasSeed(Items.CARROT)) {
                level.setBlock(cropPos, Blocks.CARROTS.defaultBlockState(), 3);
                removeSeed(Items.CARROT);
            }
        } else if (block == Blocks.POTATOES) {
            giveItem(Items.POTATO, 2 + rng.nextInt(3));
            if (hasSeed(Items.POTATO)) {
                level.setBlock(cropPos, Blocks.POTATOES.defaultBlockState(), 3);
                removeSeed(Items.POTATO);
            }
        } else if (block == Blocks.BEETROOTS) {
            giveItem(Items.BEETROOT, 1);
            giveItem(Items.BEETROOT_SEEDS, 1 + rng.nextInt(2));
            if (hasSeed(Items.BEETROOT_SEEDS)) {
                level.setBlock(cropPos, Blocks.BEETROOTS.defaultBlockState(), 3);
                removeSeed(Items.BEETROOT_SEEDS);
            }
        }
    }

    private void doPlant(ServerLevel level) {
        BlockPos cropPos = targetPos.above();
        // Determine which seed we have and what crop to place
        Item seedItem = null;
        Block cropBlock = null;
        if (hasSeed(Items.WHEAT_SEEDS)) {
            seedItem = Items.WHEAT_SEEDS;
            cropBlock = Blocks.WHEAT;
        } else if (hasSeed(Items.CARROT)) {
            seedItem = Items.CARROT;
            cropBlock = Blocks.CARROTS;
        } else if (hasSeed(Items.POTATO)) {
            seedItem = Items.POTATO;
            cropBlock = Blocks.POTATOES;
        } else if (hasSeed(Items.BEETROOT_SEEDS)) {
            seedItem = Items.BEETROOT_SEEDS;
            cropBlock = Blocks.BEETROOTS;
        }
        if (seedItem == null || cropBlock == null) return;
        // Only plant if the space above farmland is still air
        if (!level.getBlockState(cropPos).isAir()) return;
        level.setBlock(cropPos, cropBlock.defaultBlockState(), 3);
        removeSeed(seedItem);
    }

    private boolean findTarget(ServerLevel level) {
        BlockPos center = villager.blockPosition();
        // Priority 1: find mature crops to harvest
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-12, -3, -12),
                center.offset( 12,  3,  12))) {
            BlockState below = level.getBlockState(pos);
            if (!below.is(Blocks.FARMLAND)) continue;
            BlockPos cropPos = pos.above();
            BlockState cropState = level.getBlockState(cropPos);
            if (!(cropState.getBlock() instanceof CropBlock crop)) continue;
            if (crop.isMaxAge(cropState)) {
                targetPos = pos.immutable();
                isHarvest = true;
                return true;
            }
        }
        // Priority 2: find bare farmland to plant on (if we have seed)
        if (!hasSeed(Items.WHEAT_SEEDS) && !hasSeed(Items.CARROT)
                && !hasSeed(Items.POTATO) && !hasSeed(Items.BEETROOT_SEEDS)) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-12, -3, -12),
                center.offset( 12,  3,  12))) {
            BlockState below = level.getBlockState(pos);
            if (!below.is(Blocks.FARMLAND)) continue;
            BlockPos abovePos = pos.above();
            if (!level.getBlockState(abovePos).isAir()) continue;
            targetPos = pos.immutable();
            isHarvest = false;
            return true;
        }
        return false;
    }

    private boolean hasSeed(Item seed) {
        return ItemDesireSet.countItem(villager.getVillagerInventory(), seed) > 0;
    }

    private void removeSeed(Item seed) {
        var inv = villager.getVillagerInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(seed)) {
                inv.removeItem(i, 1);
                return;
            }
        }
    }

    private void giveItem(Item item, int count) {
        if (count <= 0) return;
        villager.getVillagerInventory().addItem(new ItemStack(item, count));
    }
}
