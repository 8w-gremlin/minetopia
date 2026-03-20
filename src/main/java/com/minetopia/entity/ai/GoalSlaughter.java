package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Butcher finds and kills nearby livestock, then adds raw meat to inventory.
 * Working hours: dayTime 500–11500.
 */
public class GoalSlaughter extends Goal {

    private static final double REACH_DIST_SQ = 6.25; // 2.5 blocks
    private static final int WORK_START  = 500;
    private static final int WORK_END    = 11500;
    private static final int STUCK_LIMIT = 100;

    private final MinetopiaVillager villager;

    private Animal target    = null;
    private int stuckTimer   = 0;

    public GoalSlaughter(MinetopiaVillager villager) {
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
        return target != null && target.isAlive() && stuckTimer < STUCK_LIMIT;
    }

    @Override
    public void start() {
        stuckTimer = 0;
        if (target != null) {
            villager.getNavigation().moveTo(target, 1.0);
        }
    }

    @Override
    public void stop() {
        target = null;
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) { stop(); return; }
        if (villager.distanceToSqr(target) <= REACH_DIST_SQ) {
            ServerLevel level = (ServerLevel) villager.level();
            giveDrops(target);
            target.hurt(target.damageSources().genericKill(), Float.MAX_VALUE);
            stop();
        } else {
            villager.getNavigation().moveTo(target, 1.0);
            stuckTimer++;
        }
    }

    private boolean findTarget(ServerLevel level) {
        AABB box = villager.getBoundingBox().inflate(20);
        List<Animal> candidates = level.getEntitiesOfClass(Animal.class, box,
                a -> isLivestock(a) && a.isAlive() && !a.isBaby());
        if (candidates.isEmpty()) return false;
        target = candidates.get(0);
        return true;
    }

    private boolean isLivestock(Animal a) {
        return a instanceof Cow || a instanceof Pig || a instanceof Sheep || a instanceof Chicken;
    }

    private void giveDrops(Animal a) {
        var rng = villager.getRandom();
        var inv = villager.getVillagerInventory();
        if (a instanceof Cow) {
            inv.addItem(new ItemStack(Items.BEEF, 1 + rng.nextInt(2)));
        } else if (a instanceof Pig) {
            inv.addItem(new ItemStack(Items.PORKCHOP, 1 + rng.nextInt(2)));
        } else if (a instanceof Sheep sheep) {
            inv.addItem(new ItemStack(Items.MUTTON, 1 + rng.nextInt(2)));
            if (!sheep.isSheared() && rng.nextBoolean()) {
                inv.addItem(new ItemStack(Items.WHITE_WOOL, 1));
            }
        } else if (a instanceof Chicken) {
            inv.addItem(new ItemStack(Items.CHICKEN, 1));
            if (rng.nextBoolean()) {
                inv.addItem(new ItemStack(Items.FEATHER, 1));
            }
        }
    }
}
