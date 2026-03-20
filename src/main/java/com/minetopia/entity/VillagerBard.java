package com.minetopia.entity;

import com.minetopia.economy.ProfessionTrade;
import com.minetopia.village.VillageStructureType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/** Entertains villagers and boosts their happiness via a proximity aura. */
public class VillagerBard extends MinetopiaVillager {

    private static final int AURA_TICK_RATE = 200;  // every 10 seconds
    private static final double AURA_RADIUS  = 10.0; // blocks

    private static final List<ProfessionTrade> SELL_TRADES = List.of(
            new ProfessionTrade(Items.EMERALD, 3, Items.CAKE,   1, 8,  0.05f),
            new ProfessionTrade(Items.EMERALD, 1, Items.COOKIE, 8, 16, 0.05f)
    );

    private int auraTick = 0;

    public VillagerBard(EntityType<? extends VillagerBard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MinetopiaVillager.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (++auraTick >= AURA_TICK_RATE) {
                auraTick = 0;
                tickAura();
            }
        }
    }

    private void tickAura() {
        var aabb = getBoundingBox().inflate(AURA_RADIUS);
        var nearby = level().getEntitiesOfClass(MinetopiaVillager.class, aabb, v -> v != this);
        for (var villager : nearby) {
            villager.setHappiness(villager.getHappiness() + 1);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.NOTE,
                        villager.getX(), villager.getY() + villager.getBbHeight() + 0.3,
                        villager.getZ(), 1, 0.3, 0.0, 0.3, 1.0);
            }
        }
    }

    @Override
    protected String getTradeName() { return "Bard"; }
    @Override protected VillageStructureType preferredStructureType() { return VillageStructureType.TAVERN; }

    @Override
    protected List<ProfessionTrade> getTradeDefinitions() {
        return SELL_TRADES;
    }
}
