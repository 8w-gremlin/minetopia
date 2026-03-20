package com.minetopia.entity.ai;

import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.village.VillageManager;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

/**
 * During working hours, walk to a storage chest and deposit any produce
 * items the villager is carrying (e.g. harvested wheat, carrots).
 */
public class GoalDeliverToStorage extends Goal {

    private static final double REACH_DIST_SQ = 6.25;
    private static final int WORK_START = 500;
    private static final int WORK_END   = 11500;
    private static final int STUCK_LIMIT = 100;

    private final MinetopiaVillager villager;
    private final ItemDesireSet desires;

    private BlockPos targetChest = null;
    private int stuckTimer = 0;

    public GoalDeliverToStorage(MinetopiaVillager villager, ItemDesireSet desires) {
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
        if (!desires.hasItemsToDeliver(villager.getVillagerInventory())) return false;
        return findTargetChest(serverLevel);
    }

    @Override
    public boolean canContinueToUse() {
        if (targetChest == null) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        long dayTime = serverLevel.getDayTime() % 24000;
        if (dayTime < WORK_START || dayTime > WORK_END) return false;
        if (!desires.hasItemsToDeliver(villager.getVillagerInventory())) return false;
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
        stuckTimer = 0;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetChest == null) return;
        if (villager.blockPosition().distSqr(targetChest) <= REACH_DIST_SQ) {
            doDelivery((ServerLevel) villager.level());
            stop();
        } else {
            stuckTimer++;
        }
    }

    private void doDelivery(ServerLevel level) {
        if (!(level.getBlockEntity(targetChest) instanceof Container chest)) return;
        var inv = villager.getVillagerInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !desires.getDeliverItems().contains(stack.getItem())) continue;
            ItemStack toDeposit = stack.copy();
            toDeposit = depositIntoContainer(chest, toDeposit);
            inv.setItem(i, toDeposit);
        }
    }

    /**
     * Attempts to deposit {@code stack} into {@code chest}.
     * Stacks onto matching existing slots first, then fills empty slots.
     * Returns the remainder (empty if fully deposited).
     */
    private static ItemStack depositIntoContainer(Container chest, ItemStack stack) {
        int size = chest.getContainerSize();
        // First pass: merge onto existing stacks of the same type
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            ItemStack slot = chest.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, stack)) continue;
            int space = slot.getMaxStackSize() - slot.getCount();
            if (space <= 0) continue;
            int transfer = Math.min(stack.getCount(), space);
            slot.grow(transfer);
            chest.setItem(i, slot);
            stack.shrink(transfer);
        }
        // Second pass: fill empty slots
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (!chest.getItem(i).isEmpty()) continue;
            chest.setItem(i, stack.copy());
            stack = ItemStack.EMPTY;
        }
        chest.setChanged();
        return stack;
    }

    private boolean findTargetChest(ServerLevel level) {
        var villageOpt = VillageManager.get(level)
                .findVillageById(villager.getVillageId().get());
        if (villageOpt.isEmpty()) return false;

        List<BlockPos> chests = villager.getWorkChests(villageOpt.get());
        for (BlockPos chestPos : chests) {
            if (level.getBlockEntity(chestPos) instanceof Container chest) {
                // Find any chest with at least one empty slot
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    if (chest.getItem(i).isEmpty()) {
                        targetChest = chestPos;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
