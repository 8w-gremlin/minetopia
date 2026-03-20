package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.EnumSet;

/**
 * Farmer tills nearby dirt/grass/dirt_path blocks into farmland using the hoe
 * in their mainhand. Only active during work hours and when holding a hoe.
 */
public class GoalTillSoil extends Goal {

    private static final int WORK_START  = 500;
    private static final int WORK_END    = 11500;
    private static final int SEARCH_RANGE = 10;
    private static final int STUCK_LIMIT  = 60;

    private final MinetopiaVillager villager;
    private BlockPos targetPos = null;
    private int stuckTimer = 0;

    public GoalTillSoil(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel level)) return false;
        long time = level.getDayTime() % 24000;
        if (time < WORK_START || time > WORK_END) return false;
        if (!(villager.getMainHandItem().getItem() instanceof HoeItem)) return false;
        return findTarget(level);
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        if (stuckTimer >= STUCK_LIMIT) return false;
        if (!(villager.level() instanceof ServerLevel level)) return false;
        long time = level.getDayTime() % 24000;
        return time >= WORK_START && time <= WORK_END;
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
        stuckTimer++;

        if (villager.blockPosition().distSqr(targetPos) <= 6.25) {
            ServerLevel level = (ServerLevel) villager.level();
            BlockState state = level.getBlockState(targetPos);
            if (isTillable(state)) {
                level.setBlock(targetPos, Blocks.FARMLAND.defaultBlockState(), 3);
                villager.swing(InteractionHand.MAIN_HAND);
                // Damage the hoe by 1 durability
                var hoe = villager.getMainHandItem();
                hoe.hurtAndBreak(1, villager, EquipmentSlot.MAINHAND);
            }
            stop();
        }
    }

    private boolean findTarget(ServerLevel level) {
        BlockPos origin = villager.blockPosition();
        int r = SEARCH_RANGE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-r, -2, -r),
                origin.offset( r,  2,  r))) {
            BlockState state = level.getBlockState(pos);
            if (isTillable(state) && level.getBlockState(pos.above()).isAir()
                    && hasWaterNearby(level, pos)) {
                targetPos = pos.immutable();
                return true;
            }
        }
        return false;
    }

    private static boolean isTillable(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.COARSE_DIRT);
    }

    /**
     * Returns true if there is a water source block within 4 blocks on the same Y level.
     * Mirrors vanilla farmland hydration rules.
     */
    private static boolean hasWaterNearby(ServerLevel level, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(check).is(Fluids.WATER)) return true;
        }
        return false;
    }
}
