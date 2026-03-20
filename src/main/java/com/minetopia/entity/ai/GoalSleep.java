package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Villager stops moving and idles at night (ticks 16000–24000 of the day cycle).
 */
public class GoalSleep extends Goal {

    private static final long SLEEP_START = 16000;
    private static final long SLEEP_END   = 24000;

    private final MinetopiaVillager villager;

    public GoalSleep(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        long time = villager.level().getDayTime() % 24000;
        return time >= SLEEP_START;
    }

    @Override
    public boolean canContinueToUse() {
        long time = villager.level().getDayTime() % 24000;
        return time >= SLEEP_START || time < 500;
    }

    @Override
    public void start() {
        villager.getNavigation().stop();
        villager.setDeltaMovement(0, villager.getDeltaMovement().y, 0);
    }

    @Override
    public void tick() {
        villager.getNavigation().stop();
    }
}
