package com.minetopia.entity.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class VillagerInventory extends SimpleContainer {

    public VillagerInventory() {
        super(9);
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("slot", i);
                entry.put("item", stack.save(registries));
                list.add(entry);
            }
        }
        tag.put("items", list);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("items", 9)) return; // 9 = ListTag type
        ListTag list = tag.getList("items", 10); // 10 = CompoundTag type
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < getContainerSize()) {
                ItemStack.parse(registries, entry.getCompound("item"))
                        .ifPresent(s -> setItem(slot, s));
            }
        }
    }

    /** Returns the first food item found, or empty. */
    public ItemStack findFood() {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
