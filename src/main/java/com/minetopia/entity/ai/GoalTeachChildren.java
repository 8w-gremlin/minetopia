package com.minetopia.entity.ai;

import com.minetopia.entity.VillagerChild;
import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * Teacher finds nearby VillagerChild entities and boosts their growth timer.
 * Every 40 ticks while within 4 blocks: calls child.addGrowth(60).
 */
public class GoalTeachChildren extends Goal {

    private static final double REACH_DIST_SQ    = 16.0;  // 4 blocks
    private static final double SEARCH_RADIUS    = 10.0;
    private static final double CONTINUE_RADIUS  = 15.0;
    private static final int    TEACH_INTERVAL   = 40;
    private static final int    GROWTH_PER_TEACH = 60;

    private final MinetopiaVillager villager;

    private VillagerChild target = null;
    private int teachTimer       = 0;

    public GoalTeachChildren(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        target = findChild(serverLevel, SEARCH_RADIUS);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        return villager.distanceToSqr(target) <= CONTINUE_RADIUS * CONTINUE_RADIUS;
    }

    @Override
    public void start() {
        teachTimer = 0;
        if (target != null) {
            villager.getNavigation().moveTo(target, 1.0);
        }
    }

    @Override
    public void stop() {
        target = null;
        teachTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) { stop(); return; }

        if (villager.distanceToSqr(target) > REACH_DIST_SQ) {
            villager.getNavigation().moveTo(target, 1.0);
            return;
        }

        // Within teach range
        if (++teachTimer >= TEACH_INTERVAL) {
            teachTimer = 0;
            target.addGrowth(GROWTH_PER_TEACH);

            // Happy villager particles above child
            if (villager.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() + 0.1,
                        target.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    private VillagerChild findChild(ServerLevel level, double radius) {
        List<VillagerChild> children = level.getEntitiesOfClass(
                VillagerChild.class,
                villager.getBoundingBox().inflate(radius),
                c -> c.isAlive());
        if (children.isEmpty()) return null;
        // Pick the closest child
        VillagerChild closest = null;
        double bestDist = Double.MAX_VALUE;
        for (var c : children) {
            double d = villager.distanceToSqr(c);
            if (d < bestDist) {
                bestDist = d;
                closest = c;
            }
        }
        return closest;
    }
}
