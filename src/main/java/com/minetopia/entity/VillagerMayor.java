package com.minetopia.entity;

import com.minetopia.entity.ai.GoalEatFood;
import com.minetopia.entity.ai.GoalWanderInVillage;
import com.minetopia.registry.ModItems;
import com.minetopia.village.VillageStructureType;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VillagerMayor extends MinetopiaVillager {

    public VillagerMayor(EntityType<? extends VillagerMayor> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0); // slightly tougher than a regular villager
    }

    @Override
    protected void registerGoals() {
        // Mayor stays awake at all hours — no sleep goals
        goalSelector.addGoal(1, new GoalEatFood(this));
        goalSelector.addGoal(7, new GoalWanderInVillage(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected boolean isTradingEnabled() { return true; }

    @Override
    protected String getTradeName() {
        return "Mayor";
    }

    @Override
    protected VillageStructureType preferredStructureType() { return VillageStructureType.TOWN_HALL; }

    @Override
    protected List<MerchantOffer> getExtraOffers() {
        List<MerchantOffer> offers = new ArrayList<>();

        // Structure tokens
        offers.add(trade(ModItems.STRUCTURE_HOUSE.get(),           8));
        offers.add(trade(ModItems.STRUCTURE_STORAGE.get(),         6));
        offers.add(trade(ModItems.STRUCTURE_BARRACKS.get(),       15));
        offers.add(trade(ModItems.STRUCTURE_BLACKSMITH.get(),     12));
        offers.add(trade(ModItems.STRUCTURE_KITCHEN.get(),        10));
        offers.add(trade(ModItems.STRUCTURE_LIBRARY.get(),        12));
        offers.add(trade(ModItems.STRUCTURE_BUTCHER.get(),        10));
        offers.add(trade(ModItems.STRUCTURE_MINESHAFT.get(),      15));
        offers.add(trade(ModItems.STRUCTURE_SCHOOL.get(),         12));
        offers.add(trade(ModItems.STRUCTURE_TAVERN.get(),         10));
        offers.add(trade(ModItems.STRUCTURE_GUARD_POST.get(),      8));
        offers.add(trade(ModItems.STRUCTURE_MERCHANT_STALL.get(),  8));
        offers.add(trade(ModItems.STRUCTURE_COW_PEN.get(),         8));
        offers.add(trade(ModItems.STRUCTURE_SHEEP_PEN.get(),       8));
        offers.add(trade(ModItems.STRUCTURE_PIG_PEN.get(),         8));
        offers.add(trade(ModItems.STRUCTURE_CHICKEN_COOP.get(),    6));

        // Profession tokens
        offers.add(trade(ModItems.TOKEN_FARMER.get(),      5));
        offers.add(trade(ModItems.TOKEN_MINER.get(),       8));
        offers.add(trade(ModItems.TOKEN_LUMBERJACK.get(),  6));
        offers.add(trade(ModItems.TOKEN_BLACKSMITH.get(), 10));
        offers.add(trade(ModItems.TOKEN_BUTCHER.get(),     8));
        offers.add(trade(ModItems.TOKEN_CHEF.get(),        8));
        offers.add(trade(ModItems.TOKEN_RANCHER.get(),     6));
        offers.add(trade(ModItems.TOKEN_ENCHANTER.get(),  15));
        offers.add(trade(ModItems.TOKEN_GUARD.get(),      10));
        offers.add(trade(ModItems.TOKEN_CLERIC.get(),     12));
        offers.add(trade(ModItems.TOKEN_DRUID.get(),      12));
        offers.add(trade(ModItems.TOKEN_BARD.get(),       10));
        offers.add(trade(ModItems.TOKEN_TEACHER.get(),    10));
        offers.add(trade(ModItems.TOKEN_CAPTAIN.get(),    20));
        offers.add(trade(ModItems.TOKEN_NITWIT.get(),      1));

        return offers;
    }

    /** Player pays {@code emeraldCost} emeralds and receives one of {@code token}. */
    private static MerchantOffer trade(Item token, int emeraldCost) {
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                Optional.empty(),
                new ItemStack(token),
                16, 0, 0.0f
        );
    }
}
