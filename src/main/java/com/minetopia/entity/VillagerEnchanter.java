package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalCraftBooks;
import com.minetopia.entity.ai.GoalDeliverToStorage;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructureType;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VillagerEnchanter extends MinetopiaVillager {

    /** Retrieves paper + leather to craft books; delivers finished books to storage. */
    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.PAPER,        24),
                    new ItemDesire(Items.LEATHER,       8),
                    new ItemDesire(Items.LAPIS_LAZULI, 32)
            ),
            Set.of(Items.BOOK)
    );

    /** Misc sell trades (non-enchanted). */
    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 3, Items.BOOKSHELF,    1, 12, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.LAPIS_LAZULI, 16, 16, 0.05f)
    );

    private static final ProfessionTrade BUY_PAPER  =
            new ProfessionTrade(Items.PAPER,        16, Items.EMERALD, 1, 8, 0.05f);
    private static final ProfessionTrade BUY_LAPIS =
            new ProfessionTrade(Items.LAPIS_LAZULI, 32, Items.EMERALD, 1, 8, 0.05f);

    private static final int LOW_PAPER = 8;
    private static final int LOW_LAPIS = 16;

    /** Fixed enchanted book catalogue: {enchantment key, enchant level, emerald cost} */
    private static final Object[][] BOOK_CATALOGUE = {
            { Enchantments.SHARPNESS,   1, 5 },
            { Enchantments.PROTECTION,  1, 5 },
            { Enchantments.EFFICIENCY,  1, 4 },
            { Enchantments.FORTUNE,     1, 7 },
            { Enchantments.SILK_TOUCH,  1, 8 },
            { Enchantments.FIRE_ASPECT, 1, 6 },
    };

    public VillagerEnchanter(EntityType<? extends VillagerEnchanter> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new GoalRetrieveFromStorage(this, DESIRES));
        goalSelector.addGoal(4, new GoalCraftBooks(this));
        goalSelector.addGoal(5, new GoalDeliverToStorage(this, DESIRES));
    }

    @Override
    protected String getTradeName() { return "Enchanter"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.LIBRARY; }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        List<ProfessionTrade> trades = new ArrayList<>(SELL_TRADES);
        if (getVillageId().isPresent() && !level().isClientSide()) {
            var serverLevel = (net.minecraft.server.level.ServerLevel) level();
            var villageOpt = VillageManager.get(serverLevel).findVillageById(getVillageId().get());
            if (villageOpt.isPresent()) {
                var chests = villageOpt.get().getChestPositions();
                if (countInStorage(serverLevel, chests, Items.PAPER) < LOW_PAPER)
                    trades.add(BUY_PAPER);
                if (countInStorage(serverLevel, chests, Items.LAPIS_LAZULI) < LOW_LAPIS)
                    trades.add(BUY_LAPIS);
            }
        }
        return trades;
    }

    /** Returns real enchanted book offers, built per-call from the server registry. */
    @Override
    protected List<MerchantOffer> getExtraOffers() {
        if (level().isClientSide()) return List.of();
        var registry = level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<MerchantOffer> offers = new ArrayList<>();
        for (Object[] entry : BOOK_CATALOGUE) {
            @SuppressWarnings("unchecked")
            var key = (ResourceKey<Enchantment>) entry[0];
            int enchantLevel  = (int) entry[1];
            int emeraldCost   = (int) entry[2];
            registry.get(key).ifPresent(holder -> {
                var book = new net.minecraft.world.item.ItemStack(Items.ENCHANTED_BOOK);
                var mut  = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mut.set(holder, enchantLevel);
                book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
                offers.add(new MerchantOffer(
                        new ItemCost(Items.EMERALD, emeraldCost),
                        Optional.empty(),
                        book, 0, 8, 0, 0.05f));
            });
        }
        return offers;
    }
}
