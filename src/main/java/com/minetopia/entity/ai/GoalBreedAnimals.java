package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Rancher breeds nearby livestock pairs using appropriate breeding items.
 * Working hours: dayTime 500–11500.
 */
public class GoalBreedAnimals extends Goal {

    private static final double REACH_DIST_SQ = 6.25; // 2.5 blocks
    private static final int WORK_START  = 500;
    private static final int WORK_END    = 11500;
    private static final int STUCK_LIMIT = 120;

    private final MinetopiaVillager villager;

    private Animal target1      = null;
    private Animal target2      = null;
    private Item breedingItem   = null;
    private int stuckTimer      = 0;

    public GoalBreedAnimals(MinetopiaVillager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        return findBreedingPair(serverLevel);
    }

    @Override
    public boolean canContinueToUse() {
        if (target1 == null || target2 == null) return false;
        if (!target1.isAlive() || !target2.isAlive()) return false;
        return stuckTimer < STUCK_LIMIT;
    }

    @Override
    public void start() {
        stuckTimer = 0;
        if (target1 != null) {
            villager.getNavigation().moveTo(target1, 1.0);
        }
    }

    @Override
    public void stop() {
        target1 = null;
        target2 = null;
        breedingItem = null;
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target1 == null || target2 == null) { stop(); return; }
        if (!target1.isAlive() || !target2.isAlive()) { stop(); return; }

        if (villager.distanceToSqr(target1) <= REACH_DIST_SQ) {
            // Trigger breeding on both animals
            target1.setInLove(null);
            target2.setInLove(null);
            // Consume one breeding item
            var inv = villager.getVillagerInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var stack = inv.getItem(i);
                if (stack.is(breedingItem)) {
                    inv.removeItem(i, 1);
                    break;
                }
            }
            stop();
        } else {
            villager.getNavigation().moveTo(target1, 1.0);
            stuckTimer++;
        }
    }

    private boolean findBreedingPair(ServerLevel level) {
        AABB box = villager.getBoundingBox().inflate(20);
        List<Animal> candidates = level.getEntitiesOfClass(Animal.class, box,
                a -> a.isAlive() && !a.isBaby() && !a.isInLove());

        // Try each livestock class
        return tryFindPair(candidates, Cow.class,     Items.WHEAT)
            || tryFindPair(candidates, Sheep.class,   Items.WHEAT)
            || tryFindPair(candidates, Pig.class,     Items.CARROT)
            || tryFindPair(candidates, Chicken.class, Items.WHEAT_SEEDS);
    }

    private <T extends Animal> boolean tryFindPair(List<Animal> candidates,
                                                   Class<T> type, Item food) {
        if (ItemDesireSet.countItem(villager.getVillagerInventory(), food) == 0) return false;
        List<Animal> matches = candidates.stream()
                .filter(a -> type.isInstance(a) && !a.isInLove() && !a.isBaby())
                .toList();
        if (matches.size() < 2) return false;
        target1 = matches.get(0);
        target2 = matches.get(1);
        breedingItem = food;
        return true;
    }
}
