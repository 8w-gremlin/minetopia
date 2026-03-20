package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Druid applies a bonemeal-like boost to nearby immature crops.
 * Working hours: dayTime 500–11500.
 */
public class GoalBoostCrops extends Goal {

    private static final double REACH_DIST_SQ = 6.25; // 2.5 blocks
    private static final int WORK_START  = 500;
    private static final int WORK_END    = 11500;
    private static final int STUCK_LIMIT = 60;

    private final MinetopiaVillager villager;

    /** The farmland position; crop is at targetFarmland.above(). */
    private BlockPos targetFarmland = null;
    private int stuckTimer          = 0;

    public GoalBoostCrops(MinetopiaVillager villager) {
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
        if (targetFarmland == null) return false;
        if (!(villager.level() instanceof ServerLevel)) return false;
        long dayTime = ((ServerLevel) villager.level()).getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return stuckTimer < STUCK_LIMIT;
    }

    @Override
    public void start() {
        stuckTimer = 0;
        if (targetFarmland != null) {
            villager.getNavigation().moveTo(
                    targetFarmland.getX() + 0.5, targetFarmland.getY(),
                    targetFarmland.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        targetFarmland = null;
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetFarmland == null) return;
        if (villager.blockPosition().distSqr(targetFarmland) <= REACH_DIST_SQ) {
            doBoost((ServerLevel) villager.level());
            stop();
        } else {
            stuckTimer++;
        }
    }

    private void doBoost(ServerLevel level) {
        BlockPos cropPos = targetFarmland.above();
        BlockState state = level.getBlockState(cropPos);
        if (!(state.getBlock() instanceof CropBlock)) return;
        if (!(state.getBlock() instanceof BonemealableBlock bonemealer)) return;
        if (!bonemealer.isValidBonemealTarget(level, cropPos, state)) return;

        // Apply 1-2 bonemeal applications
        int boosts = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < boosts; i++) {
            bonemealer.performBonemeal(level, level.getRandom(), cropPos, level.getBlockState(cropPos));
        }

        // Consume 1 bone meal if available (optional — druid is magical)
        var inv = villager.getVillagerInventory();
        if (ItemDesireSet.countItem(inv, Items.BONE_MEAL) > 0) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i).is(Items.BONE_MEAL)) {
                    inv.removeItem(i, 1);
                    break;
                }
            }
        }

        // Happy villager particles at crop
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5,
                4, 0.3, 0.3, 0.3, 0.01);
    }

    private boolean findTarget(ServerLevel level) {
        BlockPos center = villager.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-8, -3, -8),
                center.offset( 8,  3,  8))) {
            BlockState below = level.getBlockState(pos);
            if (!below.is(Blocks.FARMLAND)) continue;
            BlockPos cropPos = pos.above();
            BlockState cropState = level.getBlockState(cropPos);
            if (!(cropState.getBlock() instanceof CropBlock crop)) continue;
            if (crop.isMaxAge(cropState)) continue; // skip mature crops
            targetFarmland = pos.immutable();
            return true;
        }
        return false;
    }
}
