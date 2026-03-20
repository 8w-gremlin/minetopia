package com.minetopia.registry;

import com.minetopia.MinetopiaMod;
import com.minetopia.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MinetopiaMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VillagerFarmer>>    FARMER     = register("farmer",     VillagerFarmer::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerMiner>>     MINER      = register("miner",      VillagerMiner::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerLumberjack>> LUMBERJACK = register("lumberjack", VillagerLumberjack::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerBlacksmith>> BLACKSMITH = register("blacksmith", VillagerBlacksmith::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerButcher>>   BUTCHER    = register("butcher",    VillagerButcher::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerChef>>      CHEF       = register("chef",       VillagerChef::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerRancher>>   RANCHER    = register("rancher",    VillagerRancher::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerEnchanter>> ENCHANTER  = register("enchanter",  VillagerEnchanter::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerGuard>>     GUARD      = register("guard",      VillagerGuard::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerCleric>>    CLERIC     = register("cleric",     VillagerCleric::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerDruid>>     DRUID      = register("druid",      VillagerDruid::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerBard>>      BARD       = register("bard",       VillagerBard::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerTeacher>>   TEACHER    = register("teacher",    VillagerTeacher::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerCaptain>>   CAPTAIN    = register("captain",    VillagerCaptain::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerChild>>     CHILD      = register("child",      VillagerChild::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerNitwit>>    NITWIT     = register("nitwit",     VillagerNitwit::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerNomad>>     NOMAD      = register("nomad",      VillagerNomad::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VillagerMayor>>    MAYOR      = register("mayor",      VillagerMayor::new);

    private static <T extends MinetopiaVillager> DeferredHolder<EntityType<?>, EntityType<T>>
    register(String name, EntityType.EntityFactory<T> factory) {
        return ENTITY_TYPES.register(name, rl ->
                EntityType.Builder.of(factory, MobCategory.CREATURE)
                        .sized(0.6f, 1.95f)
                        .eyeHeight(1.82f)
                        .clientTrackingRange(8)
                        .build(MinetopiaMod.MODID + ":" + name)
        );
    }
}
