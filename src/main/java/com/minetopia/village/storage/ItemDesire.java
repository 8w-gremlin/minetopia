package com.minetopia.village.storage;

import net.minecraft.world.item.Item;

/**
 * A single item that a villager wants to keep stocked in their personal inventory.
 * The villager will retrieve from storage until they hold at least {@code desiredCount}.
 */
public record ItemDesire(Item item, int desiredCount) {

    public boolean isSatisfied(int currentCount) {
        return currentCount >= desiredCount;
    }
}
