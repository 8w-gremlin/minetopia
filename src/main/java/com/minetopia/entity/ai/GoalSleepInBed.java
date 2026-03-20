package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * At night (day-time ≥ 13000) or when it is raining, the villager walks to the
 * nearest bed it can find within {@link #SEARCH_RADIUS} blocks and rests there
 * until morning or the rain stops.
 *
 * <p>The mayor intentionally does not register this goal — they stay awake.
 */
public class GoalSleepInBed extends Goal {

    private static final long SLEEP_START    = 13000;
    private static final int  SEARCH_RADIUS  = 48;
    /** Ticks to wait between failed bed searches (10 seconds). */
    private static final int  SEARCH_COOLDOWN = 200;
    /** Squared distance at which the villager is "at the bed" and stops moving. */
    private static final double AT_BED_DIST_SQ = 4.0;

    private final MinetopiaVillager villager;
    private BlockPos targetBed    = null;
    private int      searchTimer  = 0;

    public GoalSleepInBed(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        if (!shouldSleep()) return false;
        if (targetBed != null && bedValid(targetBed)) return true;
        if (searchTimer > 0) { searchTimer--; return false; }
        searchTimer = SEARCH_COOLDOWN;
        targetBed = findNearestBed();
        return targetBed != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!shouldSleep()) return false;
        return targetBed != null && bedValid(targetBed);
    }

    @Override
    public void start() {
        if (targetBed != null) {
            villager.getNavigation().moveTo(
                    targetBed.getX() + 0.5, targetBed.getY(), targetBed.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void tick() {
        if (targetBed == null) return;
        if (villager.distanceToSqr(Vec3.atCenterOf(targetBed)) < AT_BED_DIST_SQ) {
            villager.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        targetBed = null;
        searchTimer = 0;
    }

    private boolean shouldSleep() {
        var level = villager.level();
        long time = level.getDayTime() % 24000;
        boolean isNight = time >= SLEEP_START;
        boolean isRaining = (level instanceof ServerLevel sl) && sl.isRaining();
        return isNight || isRaining;
    }

    private boolean bedValid(BlockPos pos) {
        var state = villager.level().getBlockState(pos);
        return state.getBlock() instanceof BedBlock
                && state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    private BlockPos findNearestBed() {
        BlockPos origin = villager.blockPosition();
        BlockPos best   = null;
        double   bestDist = Double.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx += 2) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz += 2) {
                for (int dy = -3; dy <= 4; dy++) {
                    BlockPos check = origin.offset(dx, dy, dz);
                    if (bedValid(check)) {
                        double dist = origin.distSqr(check);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = check;
                        }
                    }
                }
            }
        }
        return best;
    }
}
