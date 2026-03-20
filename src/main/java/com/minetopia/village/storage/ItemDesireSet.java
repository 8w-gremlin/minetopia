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

    /** True if the inventory contains any items flagged as produce. */
    public boolean hasItemsToDeliver(SimpleContainer inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && deliverItems.contains(stack.getItem())) return true;
        }
        return false;
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
