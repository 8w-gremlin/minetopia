package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Lumberjack finds and chops log blocks within 16 blocks.
 * Working hours: dayTime 500–11500.
 */
public class GoalChopTree extends Goal {

    private static final double REACH_DIST_SQ = 6.25; // 2.5 blocks
    private static final int WORK_START  = 500;
    private static final int WORK_END    = 11500;
    private static final int STUCK_LIMIT = 100;
    /** Stop chopping and head to storage once carrying this many logs. */
    private static final int CARRY_LIMIT = 32;

    private final MinetopiaVillager villager;

    private BlockPos targetPos = null;
    private int stuckTimer     = 0;

    public GoalChopTree(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        if (!hasAxe()) return false;
        if (countLogs() >= CARRY_LIMIT) return false; // already have enough, go deliver
        return findTarget(serverLevel);
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        if (stuckTimer >= STUCK_LIMIT) return false;
        // Check the target is still a log block
        return serverLevel.getBlockState(targetPos).is(BlockTags.LOGS);
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
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos == null) return;
        if (villager.blockPosition().distSqr(targetPos) <= REACH_DIST_SQ) {
            ServerLevel level = (ServerLevel) villager.level();
            BlockState state = level.getBlockState(targetPos);
            if (state.is(BlockTags.LOGS)) {
                Item logItem = state.getBlock().asItem();
                level.destroyBlock(targetPos, false);
                villager.getVillagerInventory().addItem(new ItemStack(logItem, 1));
                villager.swing(InteractionHand.MAIN_HAND);
            }
            // After each chop: find the next log if we still want more
            targetPos = null;
            if (countLogs() < CARRY_LIMIT && findTarget(level)) {
                stuckTimer = 0;
                villager.getNavigation().moveTo(
                        targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.0);
            } else {
                stop(); // carrying enough, or no more logs nearby
            }
        } else {
            stuckTimer++;
        }
    }

    private boolean findTarget(ServerLevel level) {
        BlockPos center = villager.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-16, -4, -16),
                center.offset( 16,  4,  16))) {
            if (level.getBlockState(pos).is(BlockTags.LOGS)) {
                targetPos = pos.immutable();
                return true;
            }
        }
        return false;
    }

    private boolean hasAxe() {
        var inv = villager.getVillagerInventory();
        return ItemDesireSet.countItem(inv, Items.WOODEN_AXE)    > 0
            || ItemDesireSet.countItem(inv, Items.STONE_AXE)     > 0
            || ItemDesireSet.countItem(inv, Items.IRON_AXE)      > 0
            || ItemDesireSet.countItem(inv, Items.GOLDEN_AXE)    > 0
            || ItemDesireSet.countItem(inv, Items.DIAMOND_AXE)   > 0
            || ItemDesireSet.countItem(inv, Items.NETHERITE_AXE) > 0;
    }

    /** Total log items (any wood type) in the villager's inventory. */
    private int countLogs() {
        var inv = villager.getVillagerInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.LOGS)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
