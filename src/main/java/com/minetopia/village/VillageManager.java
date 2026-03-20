package com.minetopia.village;

import com.minetopia.economy.VillageEconomy;
import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.registry.ModEntities;
import com.minetopia.village.storage.HouseInteriorScanner;
import com.minetopia.village.storage.HouseScanResult;
import com.minetopia.village.storage.StructureInteriorScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VillageManager extends SavedData {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillageManager.class);

    private static final int GROWTH_TICKS           = 12000; // ~10 min between growth checks
    private static final int GROWTH_MIN_HAPPINESS   = 30;

    private final Map<UUID, Village>        villages        = new HashMap<>();
    private final Map<UUID, VillageEconomy> economies       = new HashMap<>();
    private final Map<UUID, Long>           nomadNextVisit  = new HashMap<>();
    private final Map<UUID, Long>           growthNextTick  = new HashMap<>();
    private final Map<UUID, Long>           mayorRespawnAt  = new HashMap<>();

    public VillageManager() {}

    // --- Persistence ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag villagesTag = new CompoundTag();
        villages.forEach((id, v) ->
                Village.CODEC.encodeStart(NbtOps.INSTANCE, v)
                        .result().ifPresent(t -> villagesTag.put(id.toString(), t)));
        tag.put("villages", villagesTag);

        CompoundTag economiesTag = new CompoundTag();
        economies.forEach((id, e) ->
                VillageEconomy.CODEC.encodeStart(NbtOps.INSTANCE, e)
                        .result().ifPresent(t -> economiesTag.put(id.toString(), t)));
        tag.put("economies", economiesTag);

        CompoundTag nomadTag = new CompoundTag();
        nomadNextVisit.forEach((id, time) -> nomadTag.putLong(id.toString(), time));
        tag.put("nomad_visits", nomadTag);

        CompoundTag growthTag = new CompoundTag();
        growthNextTick.forEach((id, time) -> growthTag.putLong(id.toString(), time));
        tag.put("growth_ticks", growthTag);

        CompoundTag mayorTag = new CompoundTag();
        mayorRespawnAt.forEach((id, time) -> mayorTag.putLong(id.toString(), time));
        tag.put("mayor_respawn", mayorTag);

        return tag;
    }

    public static VillageManager load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageManager vm = new VillageManager();

        CompoundTag villagesTag = tag.getCompound("villages");
        for (String key : villagesTag.getAllKeys()) {
            UUID id = UUID.fromString(key);
            Village.CODEC.parse(NbtOps.INSTANCE, villagesTag.get(key))
                    .result().ifPresent(v -> vm.villages.put(id, v));
        }

        CompoundTag economiesTag = tag.getCompound("economies");
        for (String key : economiesTag.getAllKeys()) {
            UUID id = UUID.fromString(key);
            VillageEconomy.CODEC.parse(NbtOps.INSTANCE, economiesTag.get(key))
                    .result().ifPresent(e -> vm.economies.put(id, e));
        }

        CompoundTag nomadTag = tag.getCompound("nomad_visits");
        for (String key : nomadTag.getAllKeys()) {
            UUID id = UUID.fromString(key);
            vm.nomadNextVisit.put(id, nomadTag.getLong(key));
        }

        CompoundTag growthTag = tag.getCompound("growth_ticks");
        for (String key : growthTag.getAllKeys()) {
            UUID id = UUID.fromString(key);
            vm.growthNextTick.put(id, growthTag.getLong(key));
        }

        CompoundTag mayorTag = tag.getCompound("mayor_respawn");
        for (String key : mayorTag.getAllKeys()) {
            UUID id = UUID.fromString(key);
            vm.mayorRespawnAt.put(id, mayorTag.getLong(key));
        }

        return vm;
    }

    public static VillageManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageManager::new, VillageManager::load, DataFixTypes.LEVEL),
                "minetopia_villages"
        );
    }

    // --- Economy tick ---

    public void tick() {
        for (Map.Entry<UUID, VillageEconomy> entry : economies.entrySet()) {
            if (entry.getValue().tick()) {
                setDirty();
            }
        }
    }

    public void tickNomads(ServerLevel level) {
        if (villages.isEmpty()) return;
        long currentTime = level.getGameTime();
        for (Village village : villages.values()) {
            UUID vid = village.getId();
            long nextVisit = nomadNextVisit.computeIfAbsent(vid,
                    id -> currentTime + 24000L + level.getRandom().nextInt(24000));
            if (currentTime >= nextVisit) {
                spawnNomad(level, village);
                nomadNextVisit.put(vid, currentTime + 24000L + level.getRandom().nextInt(24000));
                setDirty();
            }
        }
    }

    public void tickPopulationGrowth(ServerLevel level) {
        if (villages.isEmpty()) return;
        long currentTime = level.getGameTime();
        for (Village village : villages.values()) {
            UUID vid = village.getId();
            long nextGrowth = growthNextTick.computeIfAbsent(vid,
                    id -> currentTime + GROWTH_TICKS + level.getRandom().nextInt(GROWTH_TICKS / 2));
            if (currentTime < nextGrowth) continue;

            // Reschedule regardless of outcome
            growthNextTick.put(vid, currentTime + GROWTH_TICKS + level.getRandom().nextInt(GROWTH_TICKS / 2));
            setDirty();

            // Requirements: at least 2 residents, space available, has a house
            if (village.getResidents().size() < 2) continue;
            if (village.getResidents().size() >= village.getPopulationCapacity()) continue;
            if (!village.hasStructure(VillageStructureType.HOUSE)) continue;

            // Check average happiness of live residents
            int totalHappiness = 0;
            int count = 0;
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof MinetopiaVillager v
                        && v.getVillageId().isPresent()
                        && v.getVillageId().get().equals(vid)) {
                    totalHappiness += v.getHappiness();
                    count++;
                }
            }
            if (count == 0) continue;
            int avgHappiness = totalHappiness / count;
            if (avgHappiness < GROWTH_MIN_HAPPINESS) continue;

            spawnChild(level, village);
        }
    }

    private void spawnChild(ServerLevel level, Village village) {
        var child = ModEntities.CHILD.get().create(level);
        if (child == null) return;
        BlockPos center = village.getCenter();
        child.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
        child.setMale(level.getRandom().nextBoolean());
        child.setVillageId(village.getId());
        village.addResident(child.getUUID());
        level.addFreshEntity(child);
        LOGGER.debug("Minetopia: child born in village {} at {}", village.getId(), center);
    }

    private void spawnNomad(ServerLevel level, Village village) {
        var nomad = ModEntities.NOMAD.get().create(level);
        if (nomad == null) return;
        BlockPos center = village.getCenter();
        nomad.moveTo(center.getX() + 3.5, center.getY(), center.getZ() + 0.5, 0, 0);
        nomad.setMale(level.getRandom().nextBoolean());
        level.addFreshEntity(nomad);
        LOGGER.debug("Minetopia: nomad visiting village {} at {}", village.getId(), center);
    }

    // --- Mayor ---

    public void spawnMayor(ServerLevel level, Village village) {
        spawnMayor(level, village, village.getCenter());
    }

    public void spawnMayor(ServerLevel level, Village village, BlockPos spawnPos) {
        // Don't spawn a second mayor if one is already alive
        UUID existingId = village.getMayorId();
        if (existingId != null) {
            Entity existing = level.getEntity(existingId);
            if (existing != null && existing.isAlive()) {
                LOGGER.debug("Minetopia: mayor already alive for village {}", village.getId());
                return;
            }
        }
        var mayor = ModEntities.MAYOR.get().create(level);
        mayor.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        mayor.setMale(level.getRandom().nextBoolean());
        mayor.setVillageId(village.getId());
        level.addFreshEntity(mayor);
        village.setMayorId(mayor.getUUID());
        setDirty();
        LOGGER.debug("Minetopia: mayor spawned for village {} at {}", village.getId(), spawnPos);
    }

    public void scheduleMayorRespawn(ServerLevel level, UUID villageId) {
        long respawnAt = level.getGameTime() + 24000L;
        mayorRespawnAt.put(villageId, respawnAt);
        setDirty();
        // Broadcast death message to all players
        Component msg = Component.literal("§6[Minetopia]§r The mayor has been killed! A new mayor will arrive tomorrow.");
        for (var player : level.players()) player.sendSystemMessage(msg);
    }

    public void tickMayorRespawns(ServerLevel level) {
        long now = level.getGameTime();
        var it = mayorRespawnAt.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (now < entry.getValue()) continue;
            it.remove();
            findVillageById(entry.getKey()).ifPresent(village -> {
                spawnMayor(level, village);
                Component msg = Component.literal("§6[Minetopia]§r A new mayor has arrived!");
                for (var player : level.players()) player.sendSystemMessage(msg);
            });
            setDirty();
        }
    }

    // --- World scan ---

    public void rescan(ServerLevel level) {
        villages.clear();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemFrame frame)) continue;
            ItemStack stack = frame.getItem();
            if (!VillageStructureType.TOWN_HALL.matches(stack)) continue;

            BlockPos pos = frame.blockPosition();
            Village village = new Village(UUID.randomUUID(), pos);
            village.addStructure(new VillageStructure(VillageStructureType.TOWN_HALL, pos));
            villages.put(village.getId(), village);
            economies.computeIfAbsent(village.getId(), id -> new VillageEconomy());
            LOGGER.debug("Minetopia: found town hall at {}", pos);
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemFrame frame)) continue;
            ItemStack stack = frame.getItem();
            VillageStructureType.fromItem(stack).ifPresent(type -> {
                if (type == VillageStructureType.TOWN_HALL) return;
                BlockPos pos = frame.blockPosition();
                findNearestVillage(pos).ifPresent(v -> {
                    v.addStructure(new VillageStructure(type, pos));
                    Direction dir = frame.getDirection();
                    v.putStructureChests(pos, StructureInteriorScanner.findChests(level, pos, dir));
                });
            });
        }

        for (Village village : villages.values()) {
            village.getStructures().stream()
                    .filter(s -> s.type() == VillageStructureType.TOWN_HALL)
                    .findFirst()
                    .ifPresent(s -> {
                        for (Entity entity : level.getAllEntities()) {
                            if (entity instanceof ItemFrame frame
                                    && frame.blockPosition().equals(s.pos())) {
                                village.putStructureChests(s.pos(),
                                        StructureInteriorScanner.findChests(level, s.pos(), frame.getDirection()));
                                break;
                            }
                        }
                    });
        }

        // Scan house interiors for bed count and floor space
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemFrame frame)) continue;
            if (!VillageStructureType.HOUSE.matches(frame.getItem())) continue;
            BlockPos pos = frame.blockPosition();
            findNearestVillage(pos).ifPresent(v ->
                    v.setHouseData(pos, HouseInteriorScanner.scan(level, pos, frame.getDirection())));
        }

        // Re-apply glow to all registered structure frames
        Set<BlockPos> registeredFrames = new HashSet<>();
        for (Village v : villages.values()) {
            for (VillageStructure s : v.getStructures()) registeredFrames.add(s.pos());
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemFrame frame && registeredFrames.contains(frame.blockPosition())) {
                glowFrameToken(frame);
            }
        }

        LOGGER.info("Minetopia: rescan complete — {} village(s) found", villages.size());
        setDirty();
    }

    // --- Real-time event handlers ---

    public void onFrameItemPlaced(ServerLevel level, ItemFrame frame, ServerPlayer player) {
        ItemStack stack = frame.getItem();
        VillageStructureType.fromItem(stack).ifPresent(type -> {
            BlockPos pos = frame.blockPosition();
            Direction dir = frame.getDirection();

            // Require a door near the frame
            BlockPos doorBase = findDoorBase(level, pos);
            if (doorBase == null) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cNo door found§r — place a wooden door at the entrance and put the"
                            + " structure token in a frame above or beside the door."));
                }
                return;
            }

            // Frame is always on the outside — interior is directly behind the frame direction
            BlockPos interiorStart = doorBase.relative(dir.getOpposite());

            // Check floor: the block below the interior entry point must be solid
            if (level.getBlockState(interiorStart.below())
                    .getCollisionShape(level, interiorStart.below(), CollisionContext.empty()).isEmpty()) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cNo floor at door level§r — the interior floor must be level with the door base."));
                }
                return;
            }

            // Check roof: there must be a solid block above the interior within 30 blocks
            if (!hasRoof(level, interiorStart)) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cNo roof detected§r — the building must have a solid ceiling."));
                }
                return;
            }

            // Validate structure size and beds
            HouseScanResult scan = HouseInteriorScanner.scan(level, interiorStart);
            if (scan.floorSpace() < type.minFloorSpace()) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cStructure too small§r — needs " + type.minFloorSpace()
                            + " floor spaces, found " + scan.floorSpace() + "."));
                }
                return;
            }
            if (type.minBeds() > 0 && scan.bedCount() < type.minBeds()) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cNo bed found§r — house needs at least " + type.minBeds()
                            + " bed(s) inside (found " + scan.bedCount() + ")."));
                }
                return;
            }

            // Scan chests and validate count
            List<BlockPos> chests = type == VillageStructureType.HOUSE
                    ? List.of()
                    : StructureInteriorScanner.findChests(level, interiorStart);
            if (type.minChests() > 0 && chests.size() < type.minChests()) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cNot enough chests§r — " + type.getSerializedName().replace('_', ' ')
                            + " needs " + type.minChests() + " chest(s), found " + chests.size() + "."));
                }
                return;
            }

            if (type == VillageStructureType.TOWN_HALL) {
                Village village = new Village(UUID.randomUUID(), pos);
                village.addStructure(new VillageStructure(type, pos));
                village.putStructureChests(pos, chests);
                villages.put(village.getId(), village);
                economies.computeIfAbsent(village.getId(), id -> new VillageEconomy());
                spawnMayor(level, village, interiorStart);
                glowFrameToken(frame);
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§aTown Hall registered!§r Your mayor has arrived — trade with them to get started."));
                }
            } else {
                findNearestVillage(pos).ifPresent(v -> {
                    v.addStructure(new VillageStructure(type, pos));
                    if (type == VillageStructureType.HOUSE) {
                        v.setHouseData(pos, scan);
                    } else {
                        v.putStructureChests(pos, chests);
                    }
                    glowFrameToken(frame);
                    if (player != null) {
                        player.sendSystemMessage(Component.literal(
                                "§a" + type.getSerializedName().replace('_', ' ') + " registered.§r"));
                    }
                });
                if (findNearestVillage(pos).isEmpty() && player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§cNo village found nearby§r — place a Town Hall first."));
                }
            }
            setDirty();
        });
    }

    /**
     * Rescan a single structure in-place (sneak-right-click on an already-registered frame).
     * Updates chests, house bed/floor data without re-registering the structure.
     */
    public void rescanStructure(ServerLevel level, ItemFrame frame, ServerPlayer player) {
        ItemStack stack = frame.getItem();
        VillageStructureType type = VillageStructureType.fromItem(stack).orElse(null);
        if (type == null) return;

        BlockPos pos = frame.blockPosition();
        Direction dir = frame.getDirection();

        if (type == VillageStructureType.TOWN_HALL) {
            findNearestVillage(pos).ifPresent(v -> {
                v.putStructureChests(pos, StructureInteriorScanner.findChests(level, pos, dir));
                if (player != null)
                    player.sendSystemMessage(Component.literal("Town Hall rescanned."));
            });
        } else {
            findNearestVillage(pos).ifPresent(v -> {
                if (type == VillageStructureType.HOUSE) {
                    var result = HouseInteriorScanner.scan(level, pos, dir);
                    v.setHouseData(pos, result);
                    if (player != null)
                        player.sendSystemMessage(Component.literal(
                                "House rescanned — " + result.bedCount() + " bed(s), "
                                + result.floorSpace() + " floor spaces."));
                } else {
                    v.putStructureChests(pos, StructureInteriorScanner.findChests(level, pos, dir));
                    if (player != null)
                        player.sendSystemMessage(Component.literal(
                                type.getSerializedName().replace('_', ' ') + " rescanned."));
                }
                setDirty();
            });
        }
    }

    public void onFrameItemRemoved(ServerLevel level, BlockPos pos, ItemStack removedItem) {
        VillageStructureType.fromItem(removedItem).ifPresent(type -> {
            if (type == VillageStructureType.TOWN_HALL) {
                villages.entrySet().removeIf(e -> {
                    if (e.getValue().getCenter().equals(pos)) {
                        economies.remove(e.getKey());
                        nomadNextVisit.remove(e.getKey());
                        return true;
                    }
                    return false;
                });
            } else {
                findNearestVillage(pos).ifPresent(v -> {
                    v.getStructures().removeIf(s -> s.pos().equals(pos));
                    if (type == VillageStructureType.HOUSE) {
                        v.removeHouseData(pos);
                    } else {
                        v.removeStructureChests(pos);
                    }
                });
            }
            setDirty();
        });
    }

    // --- Queries ---

    public Optional<Village> findNearestVillage(BlockPos pos) {
        Village nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Village v : villages.values()) {
            double dist = v.getCenter().distSqr(pos);
            if (dist <= (double) (Village.RADIUS * Village.RADIUS) && dist < bestDist) {
                nearest = v;
                bestDist = dist;
            }
        }
        return Optional.ofNullable(nearest);
    }

    public Optional<Village> findVillageById(UUID id) {
        return Optional.ofNullable(villages.get(id));
    }

    public VillageEconomy getEconomy(UUID villageId) {
        return economies.computeIfAbsent(villageId, id -> new VillageEconomy());
    }

    public Map<UUID, Village> getVillages() {
        return Collections.unmodifiableMap(villages);
    }

    /**
     * Rebuilds transient data (chest positions or house scan) for a single ItemFrame
     * that has just loaded into the world. Called from EntityJoinLevelEvent.
     */
    public void rebuildFrameData(ServerLevel level, ItemFrame frame) {
        ItemStack stack = frame.getItem();
        VillageStructureType type = VillageStructureType.fromItem(stack).orElse(null);
        if (type == null) return;

        BlockPos pos = frame.blockPosition();
        Direction dir = frame.getDirection();

        findNearestVillage(pos).ifPresent(village -> {
            boolean registered = village.getStructures().stream().anyMatch(s -> s.pos().equals(pos));
            if (!registered) return;

            if (type == VillageStructureType.HOUSE) {
                village.setHouseData(pos, HouseInteriorScanner.scan(level, pos, dir));
            } else {
                village.putStructureChests(pos, StructureInteriorScanner.findChests(level, pos, dir));
            }
            glowFrameToken(frame);
        });
    }

    // --- Structure validation helpers ---

    /**
     * Searches a 5×5 horizontal area around the frame (Y -3 to +1) for a door upper+lower pair.
     * Robust to frame being placed from inside or outside, with wall blocks between frame and door.
     */
    private static BlockPos findDoorBase(ServerLevel level, BlockPos framePos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -3; dy <= 1; dy++) {
                    BlockPos check = framePos.offset(dx, dy, dz);
                    if (isDoorHalf(level, check, DoubleBlockHalf.UPPER)
                            && isDoorHalf(level, check.below(), DoubleBlockHalf.LOWER)) {
                        return check.below();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isDoorHalf(ServerLevel level, BlockPos pos, DoubleBlockHalf half) {
        var state = level.getBlockState(pos);
        return state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == half;
    }

    /**
     * Returns true if there is a solid block within 10 blocks above {@code pos}.
     */
    private static boolean hasRoof(ServerLevel level, BlockPos pos) {
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos above = pos.above(dy);
            var state = level.getBlockState(above);
            if (!state.getCollisionShape(level, above, CollisionContext.empty()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies an enchantment-glint override to the token item sitting inside the frame,
     * giving it the shimmer effect without adding any actual enchantment.
     */
    private static void glowFrameToken(ItemFrame frame) {
        ItemStack token = frame.getItem().copy();
        if (token.isEmpty()) return;
        token.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        frame.setItem(token);
    }
}
