package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Travelling merchant — spawned by the nomad scheduler in VillageManager
 * and automatically despawns after VISIT_DURATION ticks.
 */
public class VillagerNomad extends MinetopiaVillager {

    /** 6000 ticks = 5 minutes at 20 TPS. */
    private static final int VISIT_DURATION = 6000;

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 2, Items.PACKED_ICE,       4,  8, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.NETHER_BRICK,     8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.QUARTZ,           8, 16, 0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.PRISMARINE_SHARD, 8, 16, 0.05f)
    );

    private int visitTimer = 0;
    private boolean announced = false;

    public VillagerNomad(EntityType<? extends VillagerNomad> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (!announced) {
                announced = true;
                playSound(SoundEvents.VILLAGER_AMBIENT, 1.0f, 1.5f);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            getX(), getY() + getBbHeight() + 0.3, getZ(),
                            20, 0.5, 0.5, 0.5, 0.0);
                }
            }
            if (++visitTimer >= VISIT_DURATION) {
                discard();
            }
        }
    }

    @Override
    protected String getTradeName() { return "Nomad"; }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        return SELL_TRADES;
    }
}
