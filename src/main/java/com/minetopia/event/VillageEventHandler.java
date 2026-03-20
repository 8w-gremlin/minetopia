package com.minetopia.event;

import com.minetopia.MinetopiaMod;
import com.minetopia.village.Village;
import com.minetopia.village.VillageManager;
import com.minetopia.village.VillageStructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = MinetopiaMod.MODID)
public class VillageEventHandler {

    private static int borderTick = 0;
    private static final int BORDER_RATE = 20; // once per second

    /** Tick village economies, nomad scheduler, and border overlay every level tick (server-side only). */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        VillageManager manager = VillageManager.get(level);
        manager.tick();
        manager.tickNomads(level);
        manager.tickPopulationGrowth(level);
        manager.tickMayorRespawns(level);
        if (++borderTick >= BORDER_RATE) {
            borderTick = 0;
            tickBorderOverlay(level);
        }
    }

    /**
     * For each player holding any Minetopia item, emit an END_ROD particle ring
     * at the boundary of their nearest village. Only the holding player sees it.
     */
    private static void tickBorderOverlay(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!isMinetopiaItem(player.getMainHandItem())
                    && !isMinetopiaItem(player.getOffhandItem())) continue;

            var nearest = VillageManager.get(level).findNearestVillage(player.blockPosition());
            if (nearest.isEmpty()) continue;

            BlockPos center = nearest.get().getCenter();
            // Only show border when within 3× radius so it doesn't appear from across the world
            if (player.blockPosition().distSqr(center) > (long) (Village.RADIUS * 3) * (Village.RADIUS * 3)) continue;

            double cy = player.getY() + 0.5;
            for (int deg = 0; deg < 360; deg += 8) {
                double rad = Math.toRadians(deg);
                double px = center.getX() + 0.5 + Village.RADIUS * Math.cos(rad);
                double pz = center.getZ() + 0.5 + Village.RADIUS * Math.sin(rad);
                level.sendParticles(player, ParticleTypes.END_ROD, false,
                        px, cy, pz, 1, 0.0, 0.0, 0.0, 0.01);
            }
        }
    }

    private static boolean isMinetopiaItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && MinetopiaMod.MODID.equals(key.getNamespace());
    }

    /**
     * When an ItemFrame with a Minetopia structure token loads into the world
     * (e.g. chunk load after server restart), rebuild its transient chest/house data.
     * This avoids the timing problem of scanning at level load before chunks are ready.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (frame.getItem().isEmpty()) return;
        // Defer one tick so the frame's block position and direction are fully initialized
        level.getServer().execute(() -> VillageManager.get(level).rebuildFrameData(level, frame));
    }

    /**
     * Detect when a player right-clicks an ItemFrame — check if they put a
     * structure or town hall token into it and update the village accordingly.
     */
    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof ItemFrame frame)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ItemStack held = event.getEntity().getItemInHand(event.getHand());
        if (held.isEmpty()) return;

        ServerPlayer player = (event.getEntity() instanceof ServerPlayer sp) ? sp : null;

        // Sneak + right-click on a frame that already has a token → rescan that structure
        if (player != null && player.isShiftKeyDown()) {
            ItemStack inFrame = frame.getItem();
            if (!inFrame.isEmpty() && VillageStructureType.fromItem(inFrame).isPresent()) {
                event.setCanceled(true); // don't rotate/remove the item
                VillageManager.get(level).rescanStructure(level, frame, player);
                return;
            }
        }

        // Schedule a check on the next tick so the item is already in the frame
        level.getServer().execute(() -> {
            ItemStack inFrame = frame.getItem();
            if (!inFrame.isEmpty()) {
                VillageManager.get(level).onFrameItemPlaced(level, frame, player);
            }
        });
    }

    /**
     * Detect when an ItemFrame leaves the level (broken by any means).
     * Remove the associated structure from the village.
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Only unregister when the frame was actually destroyed (broken by player, piston, etc.)
        // shouldDestroy() is false for UNLOADED_TO_CHUNK and UNLOADED_WITH_PLAYER, which covers
        // both chunk unloads and world shutdown — the two cases that were wiping village data.
        Entity.RemovalReason reason = frame.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) return;

        ItemStack inFrame = frame.getItem();
        if (inFrame.isEmpty()) return;

        BlockPos pos = frame.blockPosition();
        VillageManager.get(level).onFrameItemRemoved(level, pos, inFrame);
    }
}
