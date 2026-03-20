package com.minetopia.registry;

import com.minetopia.MinetopiaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MinetopiaMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINETOPIA_TAB =
            CREATIVE_MODE_TABS.register("minetopia_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.minetopia"))
                    .icon(() -> new ItemStack(ModItems.TOWN_HALL_TOKEN.get()))
                    .displayItems((params, output) -> {
                        // Guide
                        output.accept(ModItems.GUIDEBOOK.get());

                        // Town Hall
                        output.accept(ModItems.TOWN_HALL_TOKEN.get());

                        // Structure tokens
                        output.accept(ModItems.STRUCTURE_HOUSE.get());
                        output.accept(ModItems.STRUCTURE_BARRACKS.get());
                        output.accept(ModItems.STRUCTURE_STORAGE.get());
                        output.accept(ModItems.STRUCTURE_BLACKSMITH.get());
                        output.accept(ModItems.STRUCTURE_KITCHEN.get());
                        output.accept(ModItems.STRUCTURE_LIBRARY.get());
                        output.accept(ModItems.STRUCTURE_BUTCHER.get());
                        output.accept(ModItems.STRUCTURE_COW_PEN.get());
                        output.accept(ModItems.STRUCTURE_SHEEP_PEN.get());
                        output.accept(ModItems.STRUCTURE_PIG_PEN.get());
                        output.accept(ModItems.STRUCTURE_CHICKEN_COOP.get());
                        output.accept(ModItems.STRUCTURE_SCHOOL.get());
                        output.accept(ModItems.STRUCTURE_TAVERN.get());
                        output.accept(ModItems.STRUCTURE_MINESHAFT.get());
                        output.accept(ModItems.STRUCTURE_GUARD_POST.get());
                        output.accept(ModItems.STRUCTURE_MERCHANT_STALL.get());

                        // Profession tokens
                        output.accept(ModItems.TOKEN_FARMER.get());
                        output.accept(ModItems.TOKEN_MINER.get());
                        output.accept(ModItems.TOKEN_LUMBERJACK.get());
                        output.accept(ModItems.TOKEN_BLACKSMITH.get());
                        output.accept(ModItems.TOKEN_BUTCHER.get());
                        output.accept(ModItems.TOKEN_CHEF.get());
                        output.accept(ModItems.TOKEN_RANCHER.get());
                        output.accept(ModItems.TOKEN_ENCHANTER.get());
                        output.accept(ModItems.TOKEN_GUARD.get());
                        output.accept(ModItems.TOKEN_CLERIC.get());
                        output.accept(ModItems.TOKEN_DRUID.get());
                        output.accept(ModItems.TOKEN_BARD.get());
                        output.accept(ModItems.TOKEN_TEACHER.get());
                        output.accept(ModItems.TOKEN_CAPTAIN.get());
                        output.accept(ModItems.TOKEN_CHILD.get());
                        output.accept(ModItems.TOKEN_NITWIT.get());
                        output.accept(ModItems.TOKEN_NOMAD.get());
                    })
                    .build());
}
