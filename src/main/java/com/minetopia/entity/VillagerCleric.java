package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalHealVillagers;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.village.VillageManager;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VillagerCleric extends MinetopiaVillager {

    /** Retrieves brewing ingredients; potions produced via trade rather than automated delivery. */
    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.GLASS_BOTTLE, 16),
                    new ItemDesire(Items.NETHER_WART,   4),
                    new ItemDesire(Items.BLAZE_POWDER,  4)
            ),
            Set.of()
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.REDSTONE,       8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.GLOWSTONE_DUST, 8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.BLAZE_POWDER,   4, 12, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.GLASS_BOTTLE, 8, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.NETHER_WART,  4, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerCleric(EntityType<? extends VillagerCleric> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new GoalRetrieveFromStorage(this, DESIRES));
        goalSelector.addGoal(5, new GoalHealVillagers(this));
    }

    @Override
    protected String getTradeName() { return "Cleric"; }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        List<ProfessionTrade> trades = new ArrayList<>(SELL_TRADES);
        if (getVillageId().isPresent() && !level().isClientSide()) {
            var serverLevel = (net.minecraft.server.level.ServerLevel) level();
            var villageOpt = VillageManager.get(serverLevel).findVillageById(getVillageId().get());
            if (villageOpt.isPresent()) {
                var chests = villageOpt.get().getChestPositions();
                for (ProfessionTrade bt : BUY_TRADES) {
                    if (countInStorage(serverLevel, chests, bt.costItem()) < LOW_STOCK_THRESHOLD)
                        trades.add(bt);
                }
            }
        }
        return trades;
    }
}
