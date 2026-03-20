package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.VillageManager;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

/**
 * During working hours, walk to a storage chest and pull items that satisfy
 * an unsatisfied retrieve desire (e.g. seeds, hoe).
 */
public class GoalRetrieveFromStorage extends Goal {

    private static final double REACH_DIST_SQ = 6.25;  // 2.5 blocks
    private static final int WORK_START = 500;
    private static final int WORK_END   = 11500;
    /** Give up if we haven't reached the chest within this many ticks. */
    private static final int STUCK_LIMIT = 100;

    private final MinetopiaVillager villager;
    private final ItemDesireSet desires;

    private BlockPos targetChest = null;
    private ItemDesire targetDesire = null;
    private int stuckTimer = 0;

    public GoalRetrieveFromStorage(MinetopiaVillager villager, ItemDesireSet desires) {
        this.villager = villager;
        this.desires = desires;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        if (villager.getVillageId().isEmpty()) return false;
        if (!desires.hasUnsatisfiedRetrieval(villager.getVillagerInventory())) return false;
        return findTargetChest(serverLevel);
    }

    @Override
    public boolean canContinueToUse() {
        if (targetChest == null || targetDesire == null) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        if (targetDesire.isSatisfied(ItemDesireSet.countItem(villager.getVillagerInventory(), targetDesire.item()))) return false;
        return stuckTimer < STUCK_LIMIT;
    }

    @Override
    public void start() {
        stuckTimer = 0;
        if (targetChest != null) {
            villager.getNavigation().moveTo(
                    targetChest.getX() + 0.5, targetChest.getY(), targetChest.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        targetChest = null;
        targetDesire = null;
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetChest == null) return;
        if (villager.blockPosition().distSqr(targetChest) <= REACH_DIST_SQ) {
            doRetrieval((ServerLevel) villager.level());
            stop();
        } else {
            stuckTimer++;
        }
    }

    private void doRetrieval(ServerLevel level) {
        if (!(level.getBlockEntity(targetChest) instanceof Container chest)) return;
        Item wanted = targetDesire.item();
        int toTake = targetDesire.desiredCount()
                - ItemDesireSet.countItem(villager.getVillagerInventory(), wanted);
        if (toTake <= 0) return;

        for (int i = 0; i < chest.getContainerSize() && toTake > 0; i++) {
            ItemStack slot = chest.getItem(i);
            if (!slot.is(wanted)) continue;
            int take = Math.min(slot.getCount(), toTake);
            ItemStack taken = chest.removeItem(i, take);
            ItemStack remainder = villager.getVillagerInventory().addItem(taken);
            if (!remainder.isEmpty()) {
                // Inventory full — put remainder back into the same slot
                chest.setItem(i, remainder);
                break;
            }
            toTake -= take;
        }
    }

    private boolean findTargetChest(ServerLevel level) {
        var villageOpt = VillageManager.get(level)
                .findVillageById(villager.getVillageId().get());
        if (villageOpt.isEmpty()) return false;

        List<BlockPos> chests = villager.getWorkChests(villageOpt.get());
        if (chests.isEmpty()) return false;

        for (ItemDesire desire : desires.getRetrieveDesires()) {
            int current = ItemDesireSet.countItem(villager.getVillagerInventory(), desire.item());
            if (desire.isSatisfied(current)) continue;

            for (BlockPos chestPos : chests) {
                if (!(level.getBlockEntity(chestPos) instanceof Container chest)) continue;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    if (chest.getItem(i).is(desire.item())) {
                        targetChest = chestPos;
                        targetDesire = desire;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
