package com.minetopia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class GuidebookClientHelper {

    private GuidebookClientHelper() {}

    private static final List<Component> PAGES = List.of(
        Component.literal(
            "§6§lMinetopia§r\n\n" +
            "Build your own village by placing token items in item frames on your buildings.\n\n" +
            "Right-click this book any time to re-read the guide."),

        Component.literal(
            "§l§nStep 1: Town Hall§r\n\n" +
            "Craft a §6Town Hall Token§r:\n" +
            "§eTop:§r Hay Hay Hay\n" +
            "§eMiddle:§r Glass §aEmerald Block§r Glass\n" +
            "§eBottom:§r Stone Brick ×3\n\n" +
            "Place it in a frame on your building (16+ floor spaces)."),

        Component.literal(
            "§l§nThe Mayor§r\n\n" +
            "Placing the Town Hall Token spawns a §6Mayor§r.\n\n" +
            "Right-click the Mayor to buy §eall structure and profession tokens§r using emeralds.\n\n" +
            "If the Mayor is killed, a new one arrives §athe next day§r with a server broadcast."),

        Component.literal(
            "§l§nStep 2: Structures§r\n\n" +
            "Place §6Structure Tokens§r in item frames on your buildings:\n\n" +
            "§eHouse§r — homes with beds\n" +
            "§eStorage§r — shared chests\n" +
            "§eBarracks§r — for guards\n" +
            "§eBlacksmith, Kitchen, Tavern…§r\n\n" +
            "The building must be enclosed — the token scans the interior when placed."),

        Component.literal(
            "§l§nStructure Sizes§r\n\n" +
            "Minimum walkable floor spaces:\n" +
            "§eTown Hall§r — 16\n" +
            "§eBarracks§r — 12\n" +
            "§eBlacksmith, Kitchen,\nLibrary, etc.§r — 8\n" +
            "§eHouse§r — 4 + §a1 bed§r\n" +
            "§eStorage§r — 4\n\n" +
            "Rejected structures get a chat message explaining why."),

        Component.literal(
            "§l§nRescanning§r\n\n" +
            "Added chests, beds, or rooms after placing a token?\n\n" +
            "§eSneaking + right-click§r the item frame to rescan.\n\n" +
            "The game tells you the updated bed count and floor space. Changes apply immediately — no reload needed."),

        Component.literal(
            "§l§nHousing§r\n\n" +
            "A §eHouse§r token scans the interior for beds and walkable floor across §aall floors§r.\n\n" +
            "Beds inside = villager capacity.\n\n" +
            "Too many beds per floor space → §covercrowded§r → happiness falls."),

        Component.literal(
            "§l§nStep 3: Villagers§r\n\n" +
            "Right-click the §aground§r while holding a §6Profession Token§r to spawn that villager.\n\n" +
            "The village needs §ehousing capacity§r (empty bed slots) or the spawn is refused."),

        Component.literal(
            "§l§nEconomy§r\n\n" +
            "Right-click any villager to trade.\n\n" +
            "Prices shift with §esupply and demand§r over time.\n\n" +
            "§aHappy§r villagers offer better prices than unhappy ones."),

        Component.literal(
            "§l§nHappiness§r\n\n" +
            "Villagers are happier when:\n" +
            "§a✔§r Well fed (hunger ≥ 90)\n" +
            "§a✔§r Roomy housing\n" +
            "§a✔§r Near a Bard\n\n" +
            "§cHappy villagers resist starvation and give better prices.§r"),

        Component.literal(
            "§l§nNomads§r\n\n" +
            "Every day or two a §eNomad§r visits, announced with particles and sound.\n\n" +
            "Right-click them to trade.\n\n" +
            "They leave after a short stay and are §cnot§r village residents.")
    );

    public static void openScreen() {
        Minecraft.getInstance().setScreen(
                new BookViewScreen(new BookViewScreen.BookAccess(PAGES)));
    }
}
