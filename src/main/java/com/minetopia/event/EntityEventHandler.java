package com.minetopia.event;

import com.minetopia.MinetopiaMod;
import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.entity.VillagerMayor;
import com.minetopia.registry.ModEntities;
import com.minetopia.registry.ModItems;
import com.minetopia.village.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = MinetopiaMod.MODID)
public class EntityEventHandler {

    /**
     * Right-click ground with a profession token to spawn that villager type.
     * The villager is added to the nearest Minetopia village within radius,
     * provided the village has housing capacity available.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ItemStack held = event.getEntity().getMainHandItem();
        if (held.isEmpty()) return;

        if (!isProfessionToken(held)) return;

        BlockHitResult hit = event.getHitVec();
        BlockPos spawnPos = hit.getBlockPos().relative(hit.getDirection());

        VillageManager manager = VillageManager.get(level);
        var village = manager.findNearestVillage(spawnPos);
        if (village.isEmpty()) {
            event.getEntity().displayClientMessage(
                    Component.literal("§cNo village found nearby§r — place a Town Hall first."), true);
            event.setCanceled(true);
            return;
        }

        // Enforce population cap when housing structures exist
        int cap = village.get().getPopulationCapacity();
        if (cap > 0 && village.get().getResidents().size() >= cap) {
            event.getEntity().displayClientMessage(
                    Component.literal("§cThis village is full! Build more homes or barracks."), true);
            event.setCanceled(true);
            return;
        }

        var entityType = professionTokenToEntityType(held);
        if (entityType == null) return;

        var villager = entityType.create(level);
        if (villager == null) return;

        villager.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        villager.setMale(level.random.nextBoolean());
        villager.setVillageId(village.get().getId());
        village.get().addResident(villager.getUUID());
        manager.setDirty();

        level.addFreshEntity(villager);

        if (!event.getEntity().isCreative()) {
            held.shrink(1);
        }

        event.setCanceled(true);
    }

    /**
     * When a Minetopia villager dies, remove them from their village's resident list
     * so the population count stays accurate.
     */
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MinetopiaVillager villager)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        VillageManager manager = VillageManager.get(level);
        villager.getVillageId().ifPresent(villageId -> {
            manager.findVillageById(villageId).ifPresent(village -> {
                village.removeResident(villager.getUUID());
                // If the mayor died, schedule a respawn for next in-game day
                if (villager instanceof VillagerMayor) {
                    village.clearMayorId();
                    manager.scheduleMayorRespawn(level, villageId);
                }
                manager.setDirty();
            });
        });
    }

    private static boolean isProfessionToken(ItemStack stack) {
        return professionTokenToEntityType(stack) != null;
    }

    private static net.minecraft.world.entity.EntityType<? extends MinetopiaVillager>
    professionTokenToEntityType(ItemStack stack) {
        if (stack.is(ModItems.TOKEN_FARMER.get()))     return ModEntities.FARMER.get();
        if (stack.is(ModItems.TOKEN_MINER.get()))      return ModEntities.MINER.get();
        if (stack.is(ModItems.TOKEN_LUMBERJACK.get())) return ModEntities.LUMBERJACK.get();
        if (stack.is(ModItems.TOKEN_BLACKSMITH.get())) return ModEntities.BLACKSMITH.get();
        if (stack.is(ModItems.TOKEN_BUTCHER.get()))    return ModEntities.BUTCHER.get();
        if (stack.is(ModItems.TOKEN_CHEF.get()))       return ModEntities.CHEF.get();
        if (stack.is(ModItems.TOKEN_RANCHER.get()))    return ModEntities.RANCHER.get();
        if (stack.is(ModItems.TOKEN_ENCHANTER.get()))  return ModEntities.ENCHANTER.get();
        if (stack.is(ModItems.TOKEN_GUARD.get()))      return ModEntities.GUARD.get();
        if (stack.is(ModItems.TOKEN_CLERIC.get()))     return ModEntities.CLERIC.get();
        if (stack.is(ModItems.TOKEN_DRUID.get()))      return ModEntities.DRUID.get();
        if (stack.is(ModItems.TOKEN_BARD.get()))       return ModEntities.BARD.get();
        if (stack.is(ModItems.TOKEN_TEACHER.get()))    return ModEntities.TEACHER.get();
        if (stack.is(ModItems.TOKEN_CAPTAIN.get()))    return ModEntities.CAPTAIN.get();
        if (stack.is(ModItems.TOKEN_CHILD.get()))      return ModEntities.CHILD.get();
        if (stack.is(ModItems.TOKEN_NITWIT.get()))     return ModEntities.NITWIT.get();
        // TOKEN_NOMAD intentionally excluded — nomads are only spawned by VillageManager.spawnNomad()
        // so they are never added to village residents. The token is give-only (/minetopia give nomad).
        return null;
    }
}
