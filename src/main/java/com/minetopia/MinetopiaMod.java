package com.minetopia;

import com.minetopia.client.ClientSetup;
import com.minetopia.entity.*;
import com.minetopia.entity.VillagerMayor;
import com.minetopia.registry.ModCreativeTabs;
import com.minetopia.registry.ModEntities;
import com.minetopia.registry.ModItems;
import com.minetopia.registry.ModSounds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(MinetopiaMod.MODID)
public class MinetopiaMod {

    public static final String MODID = "minetopia";

    public MinetopiaMod(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerAttributes);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientSetup::onRegisterRenderers);
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FARMER.get(),     VillagerFarmer.createAttributes().build());
        event.put(ModEntities.MINER.get(),      VillagerMiner.createAttributes().build());
        event.put(ModEntities.LUMBERJACK.get(), VillagerLumberjack.createAttributes().build());
        event.put(ModEntities.BLACKSMITH.get(), VillagerBlacksmith.createAttributes().build());
        event.put(ModEntities.BUTCHER.get(),    VillagerButcher.createAttributes().build());
        event.put(ModEntities.CHEF.get(),       VillagerChef.createAttributes().build());
        event.put(ModEntities.RANCHER.get(),    VillagerRancher.createAttributes().build());
        event.put(ModEntities.ENCHANTER.get(),  VillagerEnchanter.createAttributes().build());
        event.put(ModEntities.GUARD.get(),      VillagerGuard.createAttributes().build());
        event.put(ModEntities.CLERIC.get(),     VillagerCleric.createAttributes().build());
        event.put(ModEntities.DRUID.get(),      VillagerDruid.createAttributes().build());
        event.put(ModEntities.BARD.get(),       VillagerBard.createAttributes().build());
        event.put(ModEntities.TEACHER.get(),    VillagerTeacher.createAttributes().build());
        event.put(ModEntities.CAPTAIN.get(),    VillagerCaptain.createAttributes().build());
        event.put(ModEntities.CHILD.get(),      VillagerChild.createAttributes().build());
        event.put(ModEntities.NITWIT.get(),     VillagerNitwit.createAttributes().build());
        event.put(ModEntities.NOMAD.get(),      VillagerNomad.createAttributes().build());
        event.put(ModEntities.MAYOR.get(),     VillagerMayor.createAttributes().build());
    }
}
