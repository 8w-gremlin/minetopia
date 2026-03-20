package com.minetopia.village;

import com.minetopia.registry.ModItems;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public enum VillageStructureType implements StringRepresentable {
    TOWN_HALL("town_hall"),
    STORAGE("storage"),
    HOUSE("house"),
    BARRACKS("barracks"),
    BLACKSMITH("blacksmith"),
    KITCHEN("kitchen"),
    LIBRARY("library"),
    BUTCHER("butcher"),
    COW_PEN("cow_pen"),
    SHEEP_PEN("sheep_pen"),
    PIG_PEN("pig_pen"),
    CHICKEN_COOP("chicken_coop"),
    SCHOOL("school"),
    TAVERN("tavern"),
    MINESHAFT("mineshaft"),
    GUARD_POST("guard_post"),
    MERCHANT_STALL("merchant_stall");

    public static final Codec<VillageStructureType> CODEC =
            StringRepresentable.fromEnum(VillageStructureType::values);

    private final String name;

    VillageStructureType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean matches(ItemStack stack) {
        return tokenItem().map(item -> stack.is(item)).orElse(false);
    }

    private Optional<Item> tokenItem() {
        return switch (this) {
            case TOWN_HALL       -> Optional.of(ModItems.TOWN_HALL_TOKEN.get());
            case STORAGE         -> Optional.of(ModItems.STRUCTURE_STORAGE.get());
            case HOUSE           -> Optional.of(ModItems.STRUCTURE_HOUSE.get());
            case BARRACKS        -> Optional.of(ModItems.STRUCTURE_BARRACKS.get());
            case BLACKSMITH      -> Optional.of(ModItems.STRUCTURE_BLACKSMITH.get());
            case KITCHEN         -> Optional.of(ModItems.STRUCTURE_KITCHEN.get());
            case LIBRARY         -> Optional.of(ModItems.STRUCTURE_LIBRARY.get());
            case BUTCHER         -> Optional.of(ModItems.STRUCTURE_BUTCHER.get());
            case COW_PEN         -> Optional.of(ModItems.STRUCTURE_COW_PEN.get());
            case SHEEP_PEN       -> Optional.of(ModItems.STRUCTURE_SHEEP_PEN.get());
            case PIG_PEN         -> Optional.of(ModItems.STRUCTURE_PIG_PEN.get());
            case CHICKEN_COOP    -> Optional.of(ModItems.STRUCTURE_CHICKEN_COOP.get());
            case SCHOOL          -> Optional.of(ModItems.STRUCTURE_SCHOOL.get());
            case TAVERN          -> Optional.of(ModItems.STRUCTURE_TAVERN.get());
            case MINESHAFT       -> Optional.of(ModItems.STRUCTURE_MINESHAFT.get());
            case GUARD_POST      -> Optional.of(ModItems.STRUCTURE_GUARD_POST.get());
            case MERCHANT_STALL  -> Optional.of(ModItems.STRUCTURE_MERCHANT_STALL.get());
        };
    }

    /** Minimum walkable floor-space positions required to register this structure. */
    public int minFloorSpace() {
        return switch (this) {
            case TOWN_HALL                          -> 16;
            case BARRACKS                           -> 12;
            case BLACKSMITH, KITCHEN, LIBRARY,
                 BUTCHER, MINESHAFT, SCHOOL, TAVERN -> 8;
            case HOUSE, STORAGE                     -> 4;
            default                                 -> 4;
        };
    }

    /** Minimum beds required (only meaningful for HOUSE). */
    public int minBeds() {
        return this == HOUSE ? 1 : 0;
    }

    /** Minimum chests required inside the structure on registration. */
    public int minChests() {
        return switch (this) {
            case STORAGE                                            -> 2;
            case HOUSE                                              -> 0;
            default                                                 -> 1;
        };
    }

    public static Optional<VillageStructureType> fromItem(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (VillageStructureType type : values()) {
            if (type.matches(stack)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
