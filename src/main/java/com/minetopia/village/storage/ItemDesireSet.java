package com.minetopia.village.storage;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Describes what a villager profession needs from storage (retrieve) and
 * what it produces back into storage (deliver).
 */
public class ItemDesireSet {

    private final List<ItemDesire> retrieveDesires;
    /** Items that are considered produce — the villager should always push these to storage. */
    private final Set<Item> deliverItems;

    public ItemDesireSet(List<ItemDesire> retrieveDesires, Set<Item> deliverItems) {
        this.retrieveDesires = Collections.unmodifiableList(List.copyOf(retrieveDesires));
        this.deliverItems = Collections.unmodifiableSet(Set.copyOf(deliverItems));
    }

    public List<ItemDesire> getRetrieveDesires() { return retrieveDesires; }
    public Set<Item> getDeliverItems() { return deliverItems; }

    /** True if any retrieve desire is not yet satisfied by the current inventory. */
    public boolean hasUnsatisfiedRetrieval(SimpleContainer inventory) {
        for (ItemDesire desire : retrieveDesires) {
            if (!desire.isSatisfied(countItem(inventory, desire.item()))) return true;
        }
        return false;
    }

    /**
     * Returns the desired count for {@code item}, or 0 if it is not in the retrieve list.
     * Used to calculate deliverable excess for items that are both retrieved and produced
     * (e.g. a farmer retrieves potatoes as seeds but also produces them as harvest).
     */
    public int getDesiredCount(Item item) {
        for (ItemDesire desire : retrieveDesires) {
            if (desire.item() == item) return desire.desiredCount();
        }
        return 0;
    }

    /**
     * True if the inventory contains at least {@code threshold} deliverable items,
     * counting only the excess beyond the desired amount for items that are also desired.
     * Use threshold=1 for creator professions (deliver as soon as produced).
     * Use threshold=32 for producers (batch delivery).
     */
    public boolean hasItemsToDeliver(SimpleContainer inventory, int threshold) {
        int excess = 0;
        for (Item item : deliverItems) {
            int actual  = countItem(inventory, item);
            int desired = getDesiredCount(item);
            excess += Math.max(0, actual - desired);
            if (excess >= threshold) return true;
        }
        return false;
    }

    /** Convenience overload — threshold of 1 (deliver as soon as any produce is present). */
    public boolean hasItemsToDeliver(SimpleContainer inventory) {
        return hasItemsToDeliver(inventory, 1);
    }

    /** Count how many of a given item are in the container. */
    public static int countItem(SimpleContainer inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }
}
