package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalEatFood;
import com.minetopia.entity.ai.GoalRetrieveFromStorage;
import com.minetopia.entity.ai.GoalWanderInVillage;
import com.minetopia.village.storage.ItemDesire;
import com.minetopia.village.VillageStructureType;
import com.minetopia.village.storage.ItemDesireSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/**
 * Retrieves armour, a sword, and optionally a bow with arrows.
 * Equips retrieved items automatically. Fights with a bow at range
 * when one is equipped, and with a sword in melee otherwise.
 */
public class VillagerGuard extends MinetopiaVillager implements RangedAttackMob {

    private static final ItemDesireSet DESIRES = new ItemDesireSet(
            List.of(
                    new ItemDesire(Items.BOW,             1),
                    new ItemDesire(Items.ARROW,          32),
                    new ItemDesire(Items.IRON_SWORD,      1),
                    new ItemDesire(Items.IRON_HELMET,     1),
                    new ItemDesire(Items.IRON_CHESTPLATE, 1),
                    new ItemDesire(Items.IRON_LEGGINGS,   1),
                    new ItemDesire(Items.IRON_BOOTS,      1)
            ),
            Set.of()
    );

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 1, Items.ARROW,  16, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 4, Items.SHIELD,  1, 12, 0.05f)
    );

    private static final int EQUIP_INTERVAL   = 40;
    private static final int COMBAT_MSG_COOLDOWN = 100; // 5 seconds between messages

    private int  equipTimer         = 0;
    private long lastCombatMsgTime  = 0;

    public VillagerGuard(EntityType<? extends VillagerGuard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        // Guards don't sleep — always on duty. No flee goal — they stand their ground.
        goalSelector.addGoal(1, new GoalEatFood(this));
        goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0, 20, 15.0f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(4, new GoalRetrieveFromStorage(this, DESIRES));
        goalSelector.addGoal(7, new GoalWanderInVillage(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    // --- Equipment auto-equip ---

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++equipTimer >= EQUIP_INTERVAL) {
            equipTimer = 0;
            tickEquip();
        }
    }

    /**
     * Scans the VillagerInventory and equips:
     * <ul>
     *   <li>Armor into the appropriate armor slots (only fills empty slots).
     *   <li>Weapon into the mainhand: bow preferred; sword as fallback.
     * </ul>
     */
    private void tickEquip() {
        var inv = getVillagerInventory();

        // Armor slots — fill only if currently empty
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!getItemBySlot(slot).isEmpty()) continue;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && getEquipmentSlotForItem(stack) == slot) {
                    setItemSlot(slot, stack.copy());
                    inv.removeItem(i, 1);
                    break;
                }
            }
        }

        // Mainhand weapon — prefer bow, fall back to sword
        ItemStack mainhand = getMainHandItem();
        boolean armedWithBow   = mainhand.getItem() instanceof BowItem;
        boolean armedWithSword = mainhand.getItem() instanceof SwordItem;

        if (!armedWithBow && !armedWithSword) {
            // Try bow first
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof BowItem) {
                    setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
                    inv.removeItem(i, 1);
                    return;
                }
            }
            // Fallback: sword
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof SwordItem) {
                    setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
                    inv.removeItem(i, 1);
                    return;
                }
            }
        }
    }

    // --- Combat chat alert ---

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide() && level() instanceof ServerLevel sl) {
            long now = sl.getGameTime();
            if (now - lastCombatMsgTime >= COMBAT_MSG_COOLDOWN) {
                lastCombatMsgTime = now;
                String guardName  = getDisplayName().getString();
                String targetName = target.getDisplayName().getString();
                Component msg = Component.literal(
                        "§6[Guard]§r " + guardName + " is fighting §c" + targetName + "§r!");
                sl.players().stream()
                        .filter(p -> p.distanceToSqr(this) < 2500) // 50-block radius
                        .forEach(p -> p.sendSystemMessage(msg));
            }
        }
        return hit;
    }

    // --- Ranged attack ---

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        Arrow arrow = new Arrow(level(), this, arrowStack, getMainHandItem());

        double dx = target.getX() - getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Add slight arc; inaccuracy scales with difficulty (easier = worse shots)
        float inaccuracy = 14 - level().getDifficulty().getId() * 4f;
        arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, inaccuracy);

        playSound(SoundEvents.ARROW_SHOOT, 1.0f,
                1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
    }

    // --- Sound on swing ---

    @Override
    public void swing(InteractionHand hand) {
        super.swing(hand);
        if (!level().isClientSide()) {
            playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    protected void giveStartingItems() {
        getVillagerInventory().addItem(new ItemStack(Items.WOODEN_SWORD));
    }

    @Override
    protected String getTradeName() { return "Guard"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.GUARD_POST; }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        return SELL_TRADES;
    }
}
