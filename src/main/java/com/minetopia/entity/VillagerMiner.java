package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalMineAtShaft;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructureType;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VillagerMiner extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.WOODEN_PICKAXE,    1),
                    new ItemDesire(Items.STONE_PICKAXE,     1),
                    new ItemDesire(Items.IRON_PICKAXE,      1),
                    new ItemDesire(Items.GOLDEN_PICKAXE,    1),
                    new ItemDesire(Items.DIAMOND_PICKAXE,   1),
                    new ItemDesire(Items.NETHERITE_PICKAXE, 1),
                    new ItemDesire(Items.TORCH,             16)
            ),
            Set.of(
                    Items.COBBLESTONE,
                    Items.COAL,
                    Items.RAW_IRON,
                    Items.DIAMOND
            )
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.COBBLESTONE, 16, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.COAL,         8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 2, Items.RAW_IRON,     6, 12, 0.05f),
            new ProfessionTrade(Items.EMERALD, 4, Items.DIAMOND,      1,  8, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.TORCH, 16, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerMiner(EntityType<? extends VillagerMiner> type, Level level) {
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
        goalSelector.addGoal(5, new GoalMineAtShaft(this));
    }

    @Override
    protected Class<?> getWorkToolClass() { return PickaxeItem.class; }

    @Override
    protected void giveStartingItems() {
        getVillagerInventory().addItem(new ItemStack(Items.WOODEN_PICKAXE));
        getVillagerInventory().addItem(new ItemStack(Items.TORCH, 16));
    }

    @Override
    protected String getTradeName() { return "Miner"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.MINESHAFT; }

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
