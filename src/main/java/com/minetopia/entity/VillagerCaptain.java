package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalEatFood;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.VillageStructureType;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/** Military leader — retrieves armour and shield; fights hostile mobs with higher damage than a Guard. */
public class VillagerCaptain extends MinetopiaVillager {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.IRON_SWORD,      1),
                    new ItemDesire(Items.IRON_HELMET,     1),
                    new ItemDesire(Items.IRON_CHESTPLATE, 1),
                    new ItemDesire(Items.IRON_LEGGINGS,   1),
                    new ItemDesire(Items.IRON_BOOTS,      1),
                    new ItemDesire(Items.SHIELD,          1)
            ),
            Set.of()
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 4, Items.SHIELD,       1, 12, 0.05f),
            new ProfessionTrade(Items.EMERALD, 5, Items.GOLDEN_APPLE, 1,  8, 0.05f)
    );

    public VillagerCaptain(EntityType<? extends VillagerCaptain> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void registerGoals() {
        // Captains don't sleep either
        goalSelector.addGoal(1, new GoalEatFood(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new GoalRetrieveFromStorage(this, DESIRES));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void swing(InteractionHand hand) {
        super.swing(hand);
        if (!level().isClientSide()) {
            playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    protected String getTradeName() { return "Captain"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.BARRACKS; }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        return SELL_TRADES;
    }
}
