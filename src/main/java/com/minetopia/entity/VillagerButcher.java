package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.entity.ai.GoalSlaughter;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructureType;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VillagerButcher extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.WOODEN_SWORD,    1),
                    new ItemDesire(Items.STONE_SWORD,     1),
                    new ItemDesire(Items.IRON_SWORD,      1),
                    new ItemDesire(Items.GOLDEN_SWORD,    1),
                    new ItemDesire(Items.DIAMOND_SWORD,   1),
                    new ItemDesire(Items.NETHERITE_SWORD, 1)
            ),
            Set.of(
                    Items.BEEF,
                    Items.PORKCHOP,
                    Items.CHICKEN,
                    Items.MUTTON
            )
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.BEEF,     8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.PORKCHOP, 8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.CHICKEN,  8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.MUTTON,   8, 16, 0.05f)
    );

    private static final List<ProfessionTrade> BUY_TRADES = List.of(
            new ProfessionTrade(Items.WOODEN_SWORD, 1, Items.EMERALD, 1, 8, 0.05f)
    );

    private static final int LOW_STOCK_THRESHOLD = 4;

    public VillagerButcher(EntityType<? extends VillagerButcher> type, Level level) {
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
        goalSelector.addGoal(5, new GoalSlaughter(this));
    }

    @Override
    protected String getTradeName() { return "Butcher"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.BUTCHER; }

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
