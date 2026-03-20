package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalChopTree;
import com.minetopia.entity.ai.GoalDeliverToStorage;
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

public class VillagerLumberjack extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.WOODEN_AXE,    1),
                    new ItemDesire(Items.STONE_AXE,     1),
                    new ItemDesire(Items.IRON_AXE,      1),
                    new ItemDesire(Items.GOLDEN_AXE,    1),
                    new ItemDesire(Items.DIAMOND_AXE,   1),
                    new ItemDesire(Items.NETHERITE_AXE, 1)
            ),
            Set.of(
                    Items.OAK_LOG,
                    Items.SPRUCE_LOG,
                    Items.BIRCH_LOG,
                    Items.JUNGLE_LOG,
                    Items.ACACIA_LOG,
                    Items.DARK_OAK_LOG
            )
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.OAK_LOG,    8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.SPRUCE_LOG, 8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.BIRCH_LOG,  8, 16, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.WOODEN_AXE, 1, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerLumberjack(EntityType<? extends VillagerLumberjack> type, Level level) {
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
        goalSelector.addGoal(5, new GoalChopTree(this));
    }

    @Override
    protected String getTradeName() { return "Lumberjack"; }

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
