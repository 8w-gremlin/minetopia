package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalFarmCrops;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.entity.ai.GoalTillSoil;
import com.minetopia.village.VillageManager;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VillagerFarmer extends MinetopiaVillager {

    /** Retrieve: seeds/tubers (16 each) + one hoe of any tier. */
    private static final ItemDesireSet FARMER_DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.WHEAT_SEEDS,    16),
                    new ItemDesire(Items.CARROT,         16),
                    new ItemDesire(Items.POTATO,         16),
                    new ItemDesire(Items.BEETROOT_SEEDS, 16),
                    new ItemDesire(Items.WOODEN_HOE,     1),
                    new ItemDesire(Items.STONE_HOE,      1),
                    new ItemDesire(Items.IRON_HOE,       1),
                    new ItemDesire(Items.GOLDEN_HOE,     1),
                    new ItemDesire(Items.DIAMOND_HOE,    1),
                    new ItemDesire(Items.NETHERITE_HOE,  1)
            ),
            Set.of(
                    Items.WHEAT,
                    Items.CARROT,
                    Items.POTATO,
                    Items.BEETROOT
            )
    );

    /**
     * Base trades always offered — farmer sells what they grow.
     * priceMultiplier 0.05f means demand shifts price by ~5% per demand unit.
     */
    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.WHEAT,   8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.CARROT,  12, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.POTATO,  12, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.BEETROOT, 10, 16, 0.05f)
    );

    /**
     * Buy trades — offered only when village storage is low on that item
     * (below 25% of the farmer's desired stock of 16 = fewer than 4 in storage).
     */
    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.WHEAT_SEEDS,    16, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.CARROT,         16, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.POTATO,         16, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.BEETROOT_SEEDS, 16, Items.EMERALD, 1, 8, 0.05f)
    );

    /** Low-stock threshold: offer to buy when storage has fewer than this many. */
    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerFarmer(EntityType<? extends VillagerFarmer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new GoalRetrieveFromStorage(this, FARMER_DESIRES));
        goalSelector.addGoal(4, new GoalDeliverToStorage(this, FARMER_DESIRES));
        goalSelector.addGoal(5, new GoalFarmCrops(this));
        goalSelector.addGoal(6, new GoalTillSoil(this));
    }

    @Override
    protected Class<?> getWorkToolClass() { return HoeItem.class; }

    @Override
    protected void giveStartingItems() {
        getVillagerInventory().addItem(new ItemStack(Items.WOODEN_HOE));
        getVillagerInventory().addItem(new ItemStack(Items.WHEAT_SEEDS, 8));
    }

    @Override
    protected String getTradeName() {
        return "Farmer";
    }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        List<ProfessionTrade> trades = new ArrayList<>(SELL_TRADES);

        // Only add buy offers when storage is genuinely short
        if (getVillageId().isPresent() && !level().isClientSide()) {
            var serverLevel = (net.minecraft.server.level.ServerLevel) level();
            var villageOpt = VillageManager.get(serverLevel).findVillageById(getVillageId().get());
            if (villageOpt.isPresent()) {
                var chests = villageOpt.get().getChestPositions();
                for (ProfessionTrade buyTrade : BUY_TRADES) {
                    int stored = countInStorage(serverLevel, chests, buyTrade.costItem());
                    if (stored < LOW_STOCK_THRESHOLD) {
                        trades.add(buyTrade);
                    }
                }
            }
        }

        return trades;
    }

}
