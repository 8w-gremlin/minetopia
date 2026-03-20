package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.entity.ai.GoalEatFood;
import com.minetopia.entity.ai.GoalSleep;
import com.minetopia.entity.ai.GoalSleepInBed;
import com.minetopia.entity.ai.GoalWanderInVillage;
import net.minecraft.world.entity.EquipmentSlot;
import com.minetopia.entity.inventory.VillagerInventory;
import com.minetopia.village.Village;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructureType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class MinetopiaVillager extends PathfinderMob implements Merchant {

    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(MinetopiaVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HAPPINESS =
            SynchedEntityData.defineId(MinetopiaVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_MALE =
            SynchedEntityData.defineId(MinetopiaVillager.class, EntityDataSerializers.BOOLEAN);

    private static final int HUNGER_TICK_RATE       = 100;
    private static final int STARVATION_DAMAGE_RATE = 200;
    private static final int OFFER_REBUILD_TICKS    = 4000;
    private static final int HAPPINESS_DECAY_RATE   = 400;  // -1 happiness every 20 s
    private static final int HAPPINESS_FED_RATE     = 2000; // +1 happiness when well-fed

    protected final VillagerInventory inventory = new VillagerInventory();

    private Optional<UUID> villageId = Optional.empty();
    private int hungerTimer        = 0;
    private int starvationTimer    = 0;
    private int offerRebuildTimer  = 0;
    private int happinessDecayTimer   = 0;
    private int happinessFedTimer     = 0;
    private int housingHappinessTimer = 0;
    private int soundTimer            = 0;
    private int soundInterval         = -1; // randomised on first tick
    private int toolEquipTimer        = 0;

    // Merchant state
    private Player tradingPlayer = null;
    private MerchantOffers offers = null;

    protected MinetopiaVillager(EntityType<? extends MinetopiaVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUNGER, 100);
        builder.define(DATA_HAPPINESS, 50);
        builder.define(DATA_MALE, true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new GoalEatFood(this));
        goalSelector.addGoal(2, new GoalSleepInBed(this));
        goalSelector.addGoal(3, new GoalSleep(this));           // fallback if no bed found
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Monster.class, 8.0f, 1.4, 1.8));
        goalSelector.addGoal(7, new GoalWanderInVillage(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    // --- First-spawn setup ---

    private boolean initDone = false; // transient; not saved to NBT

    /** Override in subclasses to add starting tools/items on first spawn. */
    protected void giveStartingItems() {}

    /**
     * Override in subclasses to return the Item class of the work tool
     * (e.g. {@code HoeItem.class} for farmer). Returns {@code null} for no auto-equip.
     */
    protected Class<?> getWorkToolClass() { return null; }

    /**
     * Scans VillagerInventory and equips the work tool to the mainhand slot.
     * Called every 2 seconds from tick(). Subclasses that manage their own
     * equipment (e.g. VillagerGuard) should override this.
     */
    protected void tickEquipTool() {
        Class<?> toolClass = getWorkToolClass();
        if (toolClass == null) return;
        if (toolClass.isInstance(getMainHandItem().getItem())) return; // already equipped
        var inv = getVillagerInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && toolClass.isInstance(stack.getItem())) {
                setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
                inv.removeItem(i, 1);
                return;
            }
        }
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (!initDone) {
                initDone = true;
                if (!hasCustomName()) {
                    String name = VillagerNames.pick(isMale(), getRandom()) + " the " + getTradeName();
                    setCustomName(Component.literal(name));
                    setCustomNameVisible(true);
                    giveStartingItems();
                }
            }
            tickHunger();
            tickHappiness();
            tickAmbientSound();
            tickOfferRebuild();
            if (++toolEquipTimer >= 40) {
                toolEquipTimer = 0;
                tickEquipTool();
            }
        }
    }

    private void tickHunger() {
        if (++hungerTimer >= HUNGER_TICK_RATE) {
            hungerTimer = 0;
            int hunger = getHunger();
            if (hunger > 0) {
                setHunger(hunger - 1);
            } else {
                int effectiveStarvationRate = STARVATION_DAMAGE_RATE * (getHappiness() + 50) / 100;
                if (++starvationTimer >= effectiveStarvationRate) {
                    starvationTimer = 0;
                    hurt(damageSources().starve(), 1.0f);
                }
            }
        }
    }

    private void tickHappiness() {
        // Passive decay
        if (++happinessDecayTimer >= HAPPINESS_DECAY_RATE) {
            happinessDecayTimer = 0;
            setHappiness(getHappiness() - 1);
        }
        // Bonus when well-fed (hunger >= 90)
        if (getHunger() >= 90) {
            if (++happinessFedTimer >= HAPPINESS_FED_RATE) {
                happinessFedTimer = 0;
                setHappiness(getHappiness() + 1);
            }
        } else {
            happinessFedTimer = 0;
        }
        // Housing quality — checked once per minute to avoid frequent VillageManager lookups
        if (++housingHappinessTimer >= 1200) {
            housingHappinessTimer = 0;
            tickHousingHappiness();
        }
    }

    private void tickHousingHappiness() {
        if (villageId.isEmpty() || level().isClientSide()) return;
        var sl = (net.minecraft.server.level.ServerLevel) level();
        VillageManager.get(sl).findVillageById(villageId.get()).ifPresent(village -> {
            int mod = village.getHousingHappinessMod();
            if (mod != 0) setHappiness(getHappiness() + mod);
        });
    }

    private void tickAmbientSound() {
        if (soundInterval < 0) soundInterval = 200 + random.nextInt(200);
        if (++soundTimer >= soundInterval) {
            soundTimer = 0;
            soundInterval = 200 + random.nextInt(200);
            this.playSound(SoundEvents.VILLAGER_AMBIENT, 0.5f, 1.0f + random.nextFloat() * 0.2f);
        }
    }

    private void tickOfferRebuild() {
        if (++offerRebuildTimer >= OFFER_REBUILD_TICKS) {
            offerRebuildTimer = 0;
            offers = null; // force rebuild on next getOffers() call
        }
    }

    // --- Player interaction — open trade screen ---

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        setTradingPlayer(player);
        openTradingScreen(player, Component.literal(getTradeName()), 1);
        return InteractionResult.SUCCESS;
    }

    /** Override in subclasses to return a profession-specific display name. */
    protected String getTradeName() {
        return "Villager";
    }

    // --- Merchant interface ---

    @Override
    public void setTradingPlayer(Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = buildOffers();
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers newOffers) {
        this.offers = newOffers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        // Record the trade in the village economy so pricing adjusts over time
        if (villageId.isPresent() && !level().isClientSide()) {
            var serverLevel = (net.minecraft.server.level.ServerLevel) level();
            VillageManager.get(serverLevel).getEconomy(villageId.get())
                    .recordTrade(tradeKey(offer));
            VillageManager.get(serverLevel).setDirty();
        }
        // Play trade sound (vanilla doesn't drive this for custom Merchant impls)
        this.playSound(getNotifyTradeSound(), 1.0f, 1.0f);
    }

    @Override
    public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
        // No-op — we don't track per-player reputation
    }

    @Override
    public int getVillagerXp() { return 0; }

    @Override
    public void overrideXp(int xp) {}

    @Override
    public boolean showProgressBar() { return false; }

    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_TRADE; }

    @Override
    public boolean isClientSide() { return level().isClientSide(); }

    public boolean stillValid(Player player) {
        return !isRemoved() && distanceToSqr(player) < 16.0;
    }

    // --- Trade offer building ---

    /**
     * Subclasses provide their static trade definitions.
     * Returns an empty list by default; profession classes override this.
     */
    protected List<ProfessionTrade> getTradeDefinitions() {
        return Collections.emptyList();
    }

    /**
     * Override to supply additional offers built as full MerchantOffer objects
     * (e.g. enchanted books that require ItemStack-level construction).
     * Only called server-side from {@link #buildOffers()}.
     */
    protected List<MerchantOffer> getExtraOffers() {
        return Collections.emptyList();
    }

    private MerchantOffers buildOffers() {
        MerchantOffers result = new MerchantOffers();
        if (villageId.isEmpty() || level().isClientSide()) return result;

        var serverLevel = (net.minecraft.server.level.ServerLevel) level();
        var economy = VillageManager.get(serverLevel).getEconomy(villageId.get());

        // Happiness offsets demand: happy villagers give better prices, unhappy worse
        int happinessMod = -(getHappiness() - 50) / 25; // -2 at 100 happy, +2 at 0
        for (ProfessionTrade trade : getTradeDefinitions()) {
            result.add(trade.toMerchantOffer(economy.getDemand(trade.key()) + happinessMod));
        }
        for (MerchantOffer extra : getExtraOffers()) {
            result.add(extra);
        }
        return result;
    }

    /** Derives a trade key from an executed MerchantOffer for economy tracking. */
    private static String tradeKey(MerchantOffer offer) {
        return offer.getBaseCostA().getItem().toString() + "->" + offer.getResult().getItem().toString();
    }

    /**
     * The structure type this profession works out of.
     * Override in subclasses that have a dedicated building (e.g. KITCHEN, BLACKSMITH).
     * Returning null causes the villager to fall back to all village chests.
     */
    protected VillageStructureType preferredStructureType() {
        return null;
    }

    /**
     * Returns the chest positions this villager should use for storage.
     * Uses the preferred structure's chests if available, otherwise falls back to all village chests.
     */
    public List<net.minecraft.core.BlockPos> getWorkChests(Village village) {
        VillageStructureType type = preferredStructureType();
        if (type != null) {
            var framePos = village.findStructureFrame(type);
            if (framePos.isPresent()) {
                List<net.minecraft.core.BlockPos> chests = village.getChestsForStructure(framePos.get());
                if (!chests.isEmpty()) return chests;
            }
        }
        return village.getChestPositions();
    }

    /** Counts how many of {@code item} exist across all storage chests in the given positions. */
    protected int countInStorage(net.minecraft.server.level.ServerLevel level,
                                 java.util.List<net.minecraft.core.BlockPos> chests,
                                 net.minecraft.world.item.Item item) {
        int total = 0;
        for (var pos : chests) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container chest)
                total += chest.countItem(item);
        }
        return total;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("hunger", getHunger());
        tag.putInt("happiness", getHappiness());
        tag.putBoolean("male", isMale());
        villageId.ifPresent(id -> tag.putUUID("village_id", id));
        inventory.save(tag, level().registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHunger(tag.contains("hunger") ? tag.getInt("hunger") : 100);
        setHappiness(tag.contains("happiness") ? tag.getInt("happiness") : 50);
        setMale(tag.contains("male") ? tag.getBoolean("male") : random.nextBoolean());
        villageId = tag.hasUUID("village_id") ? Optional.of(tag.getUUID("village_id")) : Optional.empty();
        inventory.load(tag, level().registryAccess());
    }

    // --- Getters / Setters ---

    public int getHunger() { return entityData.get(DATA_HUNGER); }
    public void setHunger(int v) { entityData.set(DATA_HUNGER, Math.clamp(v, 0, 100)); }

    public int getHappiness() { return entityData.get(DATA_HAPPINESS); }
    public void setHappiness(int v) { entityData.set(DATA_HAPPINESS, Math.clamp(v, 0, 100)); }

    public boolean isMale() { return entityData.get(DATA_MALE); }
    public void setMale(boolean male) { entityData.set(DATA_MALE, male); }

    public VillagerInventory getVillagerInventory() { return inventory; }

    public Optional<UUID> getVillageId() { return villageId; }
    public void setVillageId(UUID id) { this.villageId = Optional.of(id); }
}
