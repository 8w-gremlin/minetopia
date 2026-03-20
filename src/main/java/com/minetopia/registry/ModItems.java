package com.minetopia.registry;

import com.minetopia.MinetopiaMod;
import com.minetopia.item.GuidebookItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MinetopiaMod.MODID);

    // --- Guidebook ---
    public static final DeferredItem<GuidebookItem> GUIDEBOOK =
            ITEMS.registerItem("guidebook", GuidebookItem::new);

    // --- Town Hall ---
    public static final DeferredItem<Item> TOWN_HALL_TOKEN =
            ITEMS.registerSimpleItem("town_hall_token");

    // --- Structure tokens ---
    public static final DeferredItem<Item> STRUCTURE_STORAGE =
            ITEMS.registerSimpleItem("structure_storage");
    public static final DeferredItem<Item> STRUCTURE_HOUSE =
            ITEMS.registerSimpleItem("structure_house");
    public static final DeferredItem<Item> STRUCTURE_BARRACKS =
            ITEMS.registerSimpleItem("structure_barracks");
    public static final DeferredItem<Item> STRUCTURE_BLACKSMITH =
            ITEMS.registerSimpleItem("structure_blacksmith");
    public static final DeferredItem<Item> STRUCTURE_KITCHEN =
            ITEMS.registerSimpleItem("structure_kitchen");
    public static final DeferredItem<Item> STRUCTURE_LIBRARY =
            ITEMS.registerSimpleItem("structure_library");
    public static final DeferredItem<Item> STRUCTURE_BUTCHER =
            ITEMS.registerSimpleItem("structure_butcher");
    public static final DeferredItem<Item> STRUCTURE_COW_PEN =
            ITEMS.registerSimpleItem("structure_cow_pen");
    public static final DeferredItem<Item> STRUCTURE_SHEEP_PEN =
            ITEMS.registerSimpleItem("structure_sheep_pen");
    public static final DeferredItem<Item> STRUCTURE_PIG_PEN =
            ITEMS.registerSimpleItem("structure_pig_pen");
    public static final DeferredItem<Item> STRUCTURE_CHICKEN_COOP =
            ITEMS.registerSimpleItem("structure_chicken_coop");
    public static final DeferredItem<Item> STRUCTURE_SCHOOL =
            ITEMS.registerSimpleItem("structure_school");
    public static final DeferredItem<Item> STRUCTURE_TAVERN =
            ITEMS.registerSimpleItem("structure_tavern");
    public static final DeferredItem<Item> STRUCTURE_MINESHAFT =
            ITEMS.registerSimpleItem("structure_mineshaft");
    public static final DeferredItem<Item> STRUCTURE_GUARD_POST =
            ITEMS.registerSimpleItem("structure_guard_post");
    public static final DeferredItem<Item> STRUCTURE_MERCHANT_STALL =
            ITEMS.registerSimpleItem("structure_merchant_stall");

    // --- Profession tokens ---
    public static final DeferredItem<Item> TOKEN_FARMER =
            ITEMS.registerSimpleItem("token_farmer");
    public static final DeferredItem<Item> TOKEN_MINER =
            ITEMS.registerSimpleItem("token_miner");
    public static final DeferredItem<Item> TOKEN_LUMBERJACK =
            ITEMS.registerSimpleItem("token_lumberjack");
    public static final DeferredItem<Item> TOKEN_BLACKSMITH =
            ITEMS.registerSimpleItem("token_blacksmith");
    public static final DeferredItem<Item> TOKEN_BUTCHER =
            ITEMS.registerSimpleItem("token_butcher");
    public static final DeferredItem<Item> TOKEN_CHEF =
            ITEMS.registerSimpleItem("token_chef");
    public static final DeferredItem<Item> TOKEN_RANCHER =
            ITEMS.registerSimpleItem("token_rancher");
    public static final DeferredItem<Item> TOKEN_ENCHANTER =
            ITEMS.registerSimpleItem("token_enchanter");
    public static final DeferredItem<Item> TOKEN_GUARD =
            ITEMS.registerSimpleItem("token_guard");
    public static final DeferredItem<Item> TOKEN_CLERIC =
            ITEMS.registerSimpleItem("token_cleric");
    public static final DeferredItem<Item> TOKEN_DRUID =
            ITEMS.registerSimpleItem("token_druid");
    public static final DeferredItem<Item> TOKEN_BARD =
            ITEMS.registerSimpleItem("token_bard");
    public static final DeferredItem<Item> TOKEN_TEACHER =
            ITEMS.registerSimpleItem("token_teacher");
    public static final DeferredItem<Item> TOKEN_CAPTAIN =
            ITEMS.registerSimpleItem("token_captain");
    public static final DeferredItem<Item> TOKEN_NITWIT =
            ITEMS.registerSimpleItem("token_nitwit");
    public static final DeferredItem<Item> TOKEN_NOMAD =
            ITEMS.registerSimpleItem("token_nomad");
    public static final DeferredItem<Item> TOKEN_CHILD =
            ITEMS.registerSimpleItem("token_child");
}
