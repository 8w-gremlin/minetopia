package com.minetopia.entity;

import com.minetopia.registry.ModEntities;
import com.minetopia.village.VillageManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

import java.util.List;

/** Wanders and plays; grows into a random adult profession after one in-game day. */
public class VillagerChild extends MinetopiaVillager {

    /** 24000 ticks = 1 in-game day. */
    private static final int GROW_UP_TICKS = 24000;

    /**
     * Adult entity types the child may grow into.
     * Excludes Child, Nomad (visitor), and no raids so no Necromancer.
     */
    @SuppressWarnings("unchecked")
    private static final List<EntityType<? extends MinetopiaVillager>> ADULT_TYPES = List.of(
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.FARMER.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.MINER.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.LUMBERJACK.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.BLACKSMITH.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.BUTCHER.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.CHEF.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.RANCHER.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.ENCHANTER.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.GUARD.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.CLERIC.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.DRUID.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.BARD.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.TEACHER.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.CAPTAIN.get(),
            (EntityType<? extends MinetopiaVillager>) (EntityType<?>) ModEntities.NITWIT.get()
    );

    private int ageTimer = 0;

    /** Adds ticks of growth, capped at GROW_UP_TICKS. Used by GoalTeachChildren. */
    public void addGrowth(int ticks) {
        ageTimer = Math.min(ageTimer + ticks, GROW_UP_TICKS);
    }

    public VillagerChild(EntityType<? extends VillagerChild> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (++ageTimer >= GROW_UP_TICKS) {
                growUp();
            }
        }
    }

    private void growUp() {
        if (!(level() instanceof ServerLevel sl)) return;

        EntityType<? extends MinetopiaVillager> chosenType =
                ADULT_TYPES.get(random.nextInt(ADULT_TYPES.size()));

        MinetopiaVillager adult = chosenType.create(sl);
        if (adult == null) return;

        adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        getVillageId().ifPresent(adult::setVillageId);

        sl.addFreshEntity(adult);

        // Swap resident UUIDs — discard() doesn't fire LivingDeathEvent so we do it manually
        getVillageId().ifPresent(vid -> {
            var manager = VillageManager.get(sl);
            manager.findVillageById(vid).ifPresent(village -> {
                village.removeResident(getUUID());
                village.addResident(adult.getUUID());
            });
            manager.setDirty();
        });

        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("age_timer", ageTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ageTimer = tag.contains("age_timer") ? tag.getInt("age_timer") : 0;
    }

    @Override
    protected String getTradeName() { return "Child"; }
}
