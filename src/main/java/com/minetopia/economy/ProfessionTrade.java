package com.minetopia.economy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/**
 * Static definition of a single trade offered by a profession.
 * One side is always emeralds; the other is the profession's item.
 *
 * @param costItem        what the player pays (non-emerald side)
 * @param costCount       how many of costItem the player pays
 * @param resultItem      what the player receives
 * @param resultCount     how many of resultItem the player receives
 * @param maxUses         trades before restock is needed
 * @param priceMultiplier how strongly demand shifts the price (0.05 = gentle flex)
 */
public record ProfessionTrade(
        Item costItem,
        int costCount,
        Item resultItem,
        int resultCount,
        int maxUses,
        float priceMultiplier
) {
    /**
     * Builds a {@link MerchantOffer} incorporating the current demand value
     * from the village economy.
     */
    public MerchantOffer toMerchantOffer(int demand) {
        ItemCost cost = new ItemCost(costItem, costCount);
        ItemStack result = new ItemStack(resultItem, resultCount);
        // uses=0, maxUses, demand, priceMultiplier, xp=0
        return new MerchantOffer(cost, Optional.empty(), result, 0, maxUses, demand, priceMultiplier);
    }

    /** Unique string key for economy tracking (cost→result). */
    public String key() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(costItem) + "->"
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(resultItem);
    }
}
