package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalBreedAnimals;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.village.VillageManager;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VillagerRancher extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.WHEAT,       16),
                    new ItemDesire(Items.WHEAT_SEEDS, 16)
            ),
            Set.of(
                    Items.EGG,
                    Items.WHITE_WOOL,
                    Items.LEATHER,
                    Items.FEATHER
            )
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.EGG,        16, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.WHITE_WOOL,  8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.LEATHER,     4, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.FEATHER,    16, 16, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.WHEAT, 16, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerRancher(EntityType<? extends VillagerRancher> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new GoalRetrieveFromStorage(this, DESIRES));
        goalSelector.addGoal(4, new GoalDeliverToStorage(this, DESIRES));
        goalSelector.addGoal(5, new GoalBreedAnimals(this));
    }

    @Override
    protected void giveStartingItems() {
        getVillagerInventory().addItem(new ItemStack(Items.WHEAT, 16));
    }

    @Override
    protected String getTradeName() { return "Rancher"; }

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
