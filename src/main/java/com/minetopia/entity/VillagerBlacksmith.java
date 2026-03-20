package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.entity.ai.GoalSmithItems;
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

public class VillagerBlacksmith extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.IRON_INGOT, 16),
                    new ItemDesire(Items.COAL,       16)
            ),
            Set.of(
                    Items.IRON_SWORD,
                    Items.IRON_PICKAXE,
                    Items.IRON_AXE,
                    Items.IRON_SHOVEL
            )
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 7, Items.IRON_SWORD,   1, 12, 0.05f),
            new ProfessionTrade(Items.EMERALD, 9, Items.IRON_PICKAXE, 1, 12, 0.05f),
            new ProfessionTrade(Items.EMERALD, 6, Items.IRON_AXE,     1, 12, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.IRON_INGOT, 8, Items.EMERALD, 1, 8, 0.05f),
            new ProfessionTrade(Items.COAL,       8, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerBlacksmith(EntityType<? extends VillagerBlacksmith> type, Level level) {
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
        goalSelector.addGoal(5, new GoalSmithItems(this));
    }

    @Override
    protected void giveStartingItems() {
        getVillagerInventory().addItem(new ItemStack(Items.IRON_INGOT, 8));
        getVillagerInventory().addItem(new ItemStack(Items.COAL, 8));
    }

    @Override
    protected String getTradeName() { return "Blacksmith"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.BLACKSMITH; }

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
