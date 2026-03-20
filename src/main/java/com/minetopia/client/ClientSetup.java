package com.minetopia.client;

import com.minetopia.MinetopiaMod;
import com.minetopia.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ClientSetup {

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FARMER.get(),     ctx -> new VillagerRenderer<>(ctx, "farmer",     true));
        event.registerEntityRenderer(ModEntities.MINER.get(),      ctx -> new VillagerRenderer<>(ctx, "miner",      true));
        event.registerEntityRenderer(ModEntities.LUMBERJACK.get(), ctx -> new VillagerRenderer<>(ctx, "lumberjack", true));
        event.registerEntityRenderer(ModEntities.BLACKSMITH.get(), ctx -> new VillagerRenderer<>(ctx, "blacksmith", true));
        event.registerEntityRenderer(ModEntities.BUTCHER.get(),    ctx -> new VillagerRenderer<>(ctx, "butcher",    true));
        event.registerEntityRenderer(ModEntities.CHEF.get(),       ctx -> new VillagerRenderer<>(ctx, "chef",       true));
        event.registerEntityRenderer(ModEntities.RANCHER.get(),    ctx -> new VillagerRenderer<>(ctx, "rancher",    true));
        event.registerEntityRenderer(ModEntities.ENCHANTER.get(),  ctx -> new VillagerRenderer<>(ctx, "enchanter",  true));
        event.registerEntityRenderer(ModEntities.GUARD.get(),      ctx -> new VillagerRenderer<>(ctx, "guard",      true));
        event.registerEntityRenderer(ModEntities.CLERIC.get(),     ctx -> new VillagerRenderer<>(ctx, "cleric",     true));
        event.registerEntityRenderer(ModEntities.DRUID.get(),      ctx -> new VillagerRenderer<>(ctx, "druid",      true));
        event.registerEntityRenderer(ModEntities.BARD.get(),       ctx -> new VillagerRenderer<>(ctx, "bard",       true));
        event.registerEntityRenderer(ModEntities.TEACHER.get(),    ctx -> new VillagerRenderer<>(ctx, "teacher",    true));
        event.registerEntityRenderer(ModEntities.CAPTAIN.get(),    ctx -> new VillagerRenderer<>(ctx, "captain",    true));
        event.registerEntityRenderer(ModEntities.NITWIT.get(),     ctx -> new VillagerRenderer<>(ctx, "nitwit",     true));
        event.registerEntityRenderer(ModEntities.NOMAD.get(),      ctx -> new VillagerRenderer<>(ctx, "nomad",      true));
        event.registerEntityRenderer(ModEntities.CHILD.get(),      ChildVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.MAYOR.get(),      ctx -> new VillagerRenderer<>(ctx, "mayor",      true));
    }
}
