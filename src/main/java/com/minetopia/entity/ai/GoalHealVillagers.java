package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * Cleric heals nearby hurt MinetopiaVillagers.
 * Restores 5 HP when within 2.5 blocks; 200-tick cooldown between heals.
 */
public class GoalHealVillagers extends Goal {

    private static final double REACH_DIST_SQ  = 6.25; // 2.5 blocks
    private static final double SEARCH_DIST    = 12.0;
    private static final int    HEAL_COOLDOWN  = 200;
    private static final float  HEAL_AMOUNT    = 5.0f;
    private static final float  HURT_THRESHOLD = 0.75f;

    private final MinetopiaVillager villager;

    private MinetopiaVillager healTarget = null;
    private int healCooldown             = 0;

    public GoalHealVillagers(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        if (healCooldown > 0) return false;
        healTarget = findHurtVillager(serverLevel);
        return healTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (healTarget == null || !healTarget.isAlive()) return false;
        if (healCooldown > 0) return false;
        return villager.distanceToSqr(healTarget) <= SEARCH_DIST * SEARCH_DIST * 4; // 2x range for continue
    }

    @Override
    public void start() {
        if (healTarget != null) {
            villager.getNavigation().moveTo(healTarget, 1.0);
        }
    }

    @Override
    public void stop() {
        healTarget = null;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (healCooldown > 0) {
            healCooldown--;
        }
        if (healTarget == null || !healTarget.isAlive()) { stop(); return; }

        if (villager.distanceToSqr(healTarget) <= REACH_DIST_SQ) {
            doHeal();
            stop();
        } else {
            villager.getNavigation().moveTo(healTarget, 1.0);
        }
    }

    private void doHeal() {
        healTarget.heal(HEAL_AMOUNT);
        healCooldown = HEAL_COOLDOWN;

        // Sound
        villager.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);

        // Heart particles above target
        if (villager.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART,
                    healTarget.getX(), healTarget.getY() + healTarget.getBbHeight() + 0.2,
                    healTarget.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
        }
    }

    private MinetopiaVillager findHurtVillager(ServerLevel level) {
        List<MinetopiaVillager> nearby = level.getEntitiesOfClass(
                MinetopiaVillager.class,
                villager.getBoundingBox().inflate(SEARCH_DIST),
                v -> v != villager
                        && v.isAlive()
                        && v.getHealth() < v.getMaxHealth() * HURT_THRESHOLD);
        if (nearby.isEmpty()) return null;
        // Pick the most hurt one
        MinetopiaVillager best = null;
        float lowestHp = Float.MAX_VALUE;
        for (var v : nearby) {
            if (v.getHealth() < lowestHp) {
                lowestHp = v.getHealth();
                best = v;
            }
        }
        return best;
    }
}
