package com.minetopia.village;

import com.minetopia.village.storage.HouseScanResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.*;

public class Village {

    public static final int RADIUS = 120;

    /**
     * Overcrowding thresholds (beds per floor-space unit).
     * e.g. 0.25 = 1 bed per 4 standing positions → very cramped.
     */
    private static final double THRESHOLD_OVERCROWDED = 0.25;
    private static final double THRESHOLD_CRAMPED      = 0.12;
    private static final double THRESHOLD_ROOMY        = 0.06;

    private final UUID id;
    private final BlockPos center;
    private final List<VillageStructure> structures;
    private final List<UUID> residents;
    private UUID mayorId = null; // transient — set when mayor spawns, cleared on death

    /**
     * Transient — rebuilt on world load via StructureInteriorScanner; not serialized.
     * Key: item-frame BlockPos of the structure. Value: chest positions inside it.
     */
    private final Map<BlockPos, List<BlockPos>> structureChests = new HashMap<>();

    /**
     * Transient — rebuilt on world load via HouseInteriorScanner.
     * Key: item-frame BlockPos of the HOUSE structure.
     */
    private final Map<BlockPos, HouseScanResult> houseData = new HashMap<>();

    public Village(UUID id, BlockPos center) {
        this(id, center, new ArrayList<>(), new ArrayList<>(), Optional.empty());
    }

    private Village(UUID id, BlockPos center, List<VillageStructure> structures, List<UUID> residents, Optional<UUID> mayorId) {
        this.id = id;
        this.center = center;
        this.structures = new ArrayList<>(structures);
        this.residents = new ArrayList<>(residents);
        this.mayorId = mayorId.orElse(null);
    }

    public static final Codec<Village> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(v -> v.id),
                    BlockPos.CODEC.fieldOf("center").forGetter(v -> v.center),
                    VillageStructure.CODEC.listOf().fieldOf("structures").forGetter(v -> v.structures),
                    UUIDUtil.CODEC.listOf().fieldOf("residents").forGetter(v -> v.residents),
                    UUIDUtil.CODEC.optionalFieldOf("mayor_id").forGetter(v -> Optional.ofNullable(v.mayorId))
            ).apply(instance, Village::new)
    );

    public UUID getId() { return id; }
    public UUID getMayorId() { return mayorId; }
    public void setMayorId(UUID id) { this.mayorId = id; }
    public void clearMayorId() { this.mayorId = null; }
    public BlockPos getCenter() { return center; }
    public List<VillageStructure> getStructures() { return structures; }
    public List<UUID> getResidents() { return Collections.unmodifiableList(residents); }

    public void addStructure(VillageStructure structure) {
        structures.add(structure);
    }

    public void addResident(UUID residentId) {
        if (!residents.contains(residentId)) residents.add(residentId);
    }

    public void removeResident(UUID residentId) {
        residents.remove(residentId);
    }

    // --- Chest cache (transient) ---

    /** All chests across every structure — flattened. Used as fallback by professions with no dedicated building. */
    public List<BlockPos> getChestPositions() {
        List<BlockPos> all = new ArrayList<>();
        for (List<BlockPos> list : structureChests.values()) all.addAll(list);
        return Collections.unmodifiableList(all);
    }

    /** Chests belonging to the structure whose ItemFrame is at {@code framePos}. Empty list if none scanned. */
    public List<BlockPos> getChestsForStructure(BlockPos framePos) {
        return Collections.unmodifiableList(structureChests.getOrDefault(framePos, List.of()));
    }

    public void putStructureChests(BlockPos framePos, List<BlockPos> chests) {
        structureChests.put(framePos, new ArrayList<>(chests));
    }

    public void removeStructureChests(BlockPos framePos) {
        structureChests.remove(framePos);
    }

    /** Returns the frame pos of the first structure of {@code type} found, or empty. */
    public Optional<BlockPos> findStructureFrame(VillageStructureType type) {
        return structures.stream()
                .filter(s -> s.type() == type)
                .map(VillageStructure::pos)
                .findFirst();
    }

    // --- House data (transient) ---

    public void setHouseData(BlockPos framePos, HouseScanResult result) {
        houseData.put(framePos, result);
    }

    public void removeHouseData(BlockPos framePos) {
        houseData.remove(framePos);
    }

    public int getTotalBeds() {
        return houseData.values().stream().mapToInt(HouseScanResult::bedCount).sum();
    }

    public int getTotalFloorSpace() {
        return houseData.values().stream().mapToInt(HouseScanResult::floorSpace).sum();
    }

    /**
     * Beds-per-floor-space ratio across all houses.
     * Returns 0 if no houses have been scanned yet.
     */
    public double getCrowdingRatio() {
        int space = getTotalFloorSpace();
        return space == 0 ? 0.0 : (double) getTotalBeds() / space;
    }

    /**
     * -2 = severely overcrowded, -1 = cramped, 0 = neutral, +1 = roomy.
     * Returns 0 if no house data is available (no houses placed yet).
     */
    public int getHousingHappinessMod() {
        if (houseData.isEmpty()) return 0;
        double ratio = getCrowdingRatio();
        if (ratio >= THRESHOLD_OVERCROWDED) return -2;
        if (ratio >= THRESHOLD_CRAMPED)     return -1;
        if (ratio <= THRESHOLD_ROOMY && getTotalBeds() > 0) return 1;
        return 0;
    }

    // --- Queries ---

    public boolean isWithinRadius(BlockPos pos) {
        return center.distSqr(pos) <= (double) (RADIUS * RADIUS);
    }

    public boolean hasStructure(VillageStructureType type) {
        return structures.stream().anyMatch(s -> s.type() == type);
    }

    public int countStructures(VillageStructureType type) {
        return (int) structures.stream().filter(s -> s.type() == type).count();
    }

    /**
     * Population capacity = beds from all scanned houses + barracks slots.
     * Falls back to counting HOUSE structures * 2 if no scan data yet.
     */
    public int getPopulationCapacity() {
        int bedSlots = houseData.isEmpty()
                ? countStructures(VillageStructureType.HOUSE) * 2
                : getTotalBeds();
        return bedSlots + countStructures(VillageStructureType.BARRACKS) * 4;
    }
}
