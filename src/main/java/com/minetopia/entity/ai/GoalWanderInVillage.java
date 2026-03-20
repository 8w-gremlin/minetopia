package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Wanders randomly but keeps the villager near their village center.
 *
 * <ul>
 *   <li>If the villager is inside {@link #WANDER_RANGE} of the center, picks a
 *       random nearby position (biased toward the center).
 *   <li>If the villager has drifted outside {@link #WANDER_RANGE}, walks directly
 *       back toward the center.
 *   <li>If no village is known, falls back to a plain local random wander.
 * </ul>
 */
public class GoalWanderInVillage extends Goal {

    /** Maximum distance from the village center the villager will wander. */
    private static final int WANDER_RANGE = 30;
    /** Speed multiplier while wandering. */
    private static final double SPEED = 1.0;
    /** Average ticks between wander attempts (1-in-N chance per tick). */
    private static final int INTERVAL = 120;

    private final MinetopiaVillager villager;
    private Vec3 target = null;

    public GoalWanderInVillage(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        if (villager.level().getRandom().nextInt(INTERVAL) != 0) return false;
        target = pickTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !villager.getNavigation().isDone();
    }

    @Override
    public void start() {
        villager.getNavigation().moveTo(target.x, target.y, target.z, SPEED);
    }

    @Override
    public void stop() {
        target = null;
    }

    private Vec3 pickTarget() {
        if (villager.getVillageId().isEmpty() || villager.level().isClientSide()) {
            return localRandom();
        }

        var level = (ServerLevel) villager.level();
        var villageOpt = VillageManager.get(level).findVillageById(villager.getVillageId().get());
        if (villageOpt.isEmpty()) return localRandom();

        BlockPos center = villageOpt.get().getCenter();
        Vec3 centerVec = Vec3.atCenterOf(center);

        // If too far from center, walk straight back
        if (villager.distanceToSqr(centerVec) > (double) WANDER_RANGE * WANDER_RANGE) {
            return centerVec;
        }

        // Otherwise pick a random position within WANDER_RANGE, biased toward center
        Vec3 pos = DefaultRandomPos.getPosTowards(villager, WANDER_RANGE, 4, centerVec, Math.PI);
        return pos != null ? pos : centerVec;
    }

    private Vec3 localRandom() {
        return DefaultRandomPos.getPos(villager, 10, 7);
    }
}
