package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalCookFood;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructureType;
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

public class VillagerChef extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.BEEF,     8),
                    new ItemDesire(Items.PORKCHOP, 8),
                    new ItemDesire(Items.CHICKEN,  8),
                    new ItemDesire(Items.COAL,     16)
            ),
            Set.of(
                    Items.COOKED_BEEF,
                    Items.COOKED_PORKCHOP,
                    Items.COOKED_CHICKEN,
                    Items.BREAD
            )
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.COOKED_BEEF,     4, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.COOKED_PORKCHOP, 4, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.COOKED_CHICKEN,  4, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.BREAD,           6, 16, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.BEEF,     8, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.PORKCHOP, 8, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.CHICKEN,  8, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerChef(EntityType<? extends VillagerChef> type, Level level) {
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
        goalSelector.addGoal(5, new GoalCookFood(this));
    }

    @Override
    protected void giveStartingItems() {
        getVillagerInventory().addItem(new ItemStack(Items.COAL, 8));
        getVillagerInventory().addItem(new ItemStack(Items.BEEF, 4));
    }

    @Override
    protected String getTradeName() { return "Chef"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.KITCHEN; }

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
