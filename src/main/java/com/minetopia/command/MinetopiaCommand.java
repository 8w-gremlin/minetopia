package com.minetopia.command;

import com.minetopia.MinetopiaMod;
import com.minetopia.economy.VillageEconomy;
import com.minetopia.entity.MinetopiaVillager;
import com.minetopia.registry.ModItems;
import com.minetopia.village.Village;
import com.minetopia.village.VillageManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = MinetopiaMod.MODID)
public class MinetopiaCommand {

    /** All giveable token names, in a stable order for tab-completion. */
    private static final Map<String, DeferredItem<net.minecraft.world.item.Item>> TOKEN_MAP = new LinkedHashMap<>();

    static {
        TOKEN_MAP.put("town_hall",  ModItems.TOWN_HALL_TOKEN);
        TOKEN_MAP.put("storage",    ModItems.STRUCTURE_STORAGE);
        TOKEN_MAP.put("farmer",     ModItems.TOKEN_FARMER);
        TOKEN_MAP.put("miner",      ModItems.TOKEN_MINER);
        TOKEN_MAP.put("lumberjack", ModItems.TOKEN_LUMBERJACK);
        TOKEN_MAP.put("blacksmith", ModItems.TOKEN_BLACKSMITH);
        TOKEN_MAP.put("butcher",    ModItems.TOKEN_BUTCHER);
        TOKEN_MAP.put("chef",       ModItems.TOKEN_CHEF);
        TOKEN_MAP.put("rancher",    ModItems.TOKEN_RANCHER);
        TOKEN_MAP.put("enchanter",  ModItems.TOKEN_ENCHANTER);
        TOKEN_MAP.put("guard",      ModItems.TOKEN_GUARD);
        TOKEN_MAP.put("cleric",     ModItems.TOKEN_CLERIC);
        TOKEN_MAP.put("druid",      ModItems.TOKEN_DRUID);
        TOKEN_MAP.put("bard",       ModItems.TOKEN_BARD);
        TOKEN_MAP.put("teacher",    ModItems.TOKEN_TEACHER);
        TOKEN_MAP.put("captain",    ModItems.TOKEN_CAPTAIN);
        TOKEN_MAP.put("child",      ModItems.TOKEN_CHILD);
        TOKEN_MAP.put("nitwit",     ModItems.TOKEN_NITWIT);
        TOKEN_MAP.put("nomad",      ModItems.TOKEN_NOMAD);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("minetopia")
                .then(Commands.literal("version")
                    .executes(MinetopiaCommand::executeVersion))
                .then(Commands.literal("status")
                    .executes(MinetopiaCommand::executeStatus))
                .then(Commands.literal("list")
                    .executes(MinetopiaCommand::executeList))
                .then(Commands.literal("give")
                    .then(Commands.argument("token", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                                SharedSuggestionProvider.suggest(TOKEN_MAP.keySet(), builder))
                        .executes(MinetopiaCommand::executeGive)))
                .then(Commands.literal("economy")
                    .then(Commands.argument("villageId", StringArgumentType.word())
                        .executes(MinetopiaCommand::executeEconomy)))
                .then(Commands.literal("recall")
                    .executes(MinetopiaCommand::executeRecall))
        );
    }

    // --- /minetopia version ---

    private static int executeVersion(CommandContext<CommandSourceStack> ctx) {
        String version = net.neoforged.fml.ModList.get()
                .getModContainerById(MinetopiaMod.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        ctx.getSource().sendSuccess(() -> Component.literal("§6Minetopia §fv" + version), false);
        return 1;
    }

    // --- /minetopia status ---

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getLevel() instanceof ServerLevel level)) return 0;

        var pos = net.minecraft.core.BlockPos.containing(source.getPosition());
        var villageOpt = VillageManager.get(level).findNearestVillage(pos);

        if (villageOpt.isEmpty()) {
            source.sendFailure(Component.literal("No Minetopia village found nearby."));
            return 0;
        }

        Village v = villageOpt.get();
        int pop = v.getResidents().size();
        int cap = v.getPopulationCapacity();
        String capStr = cap == 0 ? "no housing" : String.valueOf(cap);
        var center = v.getCenter();
        String shortId = v.getId().toString().substring(0, 8);

        source.sendSuccess(() -> Component.literal("§6=== Minetopia Village §e" + shortId + "§6 ==="), false);
        source.sendSuccess(() -> Component.literal("§eCenter:     §f" + center.getX() + ", " + center.getY() + ", " + center.getZ()), false);
        source.sendSuccess(() -> Component.literal("§ePopulation: §f" + pop + " / " + capStr), false);
        source.sendSuccess(() -> Component.literal("§eStructures: §f" + v.getStructures().size()), false);
        source.sendSuccess(() -> Component.literal("§eChest slots: §f" + v.getChestPositions().size() + " chest(s) indexed"), false);
        return 1;
    }

    // --- /minetopia list ---

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getLevel() instanceof ServerLevel level)) return 0;

        var villages = VillageManager.get(level).getVillages();
        if (villages.isEmpty()) {
            source.sendFailure(Component.literal("No Minetopia villages exist."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6=== Villages (" + villages.size() + ") ==="), false);
        for (Village v : villages.values()) {
            var c = v.getCenter();
            int pop = v.getResidents().size();
            int cap = v.getPopulationCapacity();
            String id8 = v.getId().toString().substring(0, 8);
            source.sendSuccess(() -> Component.literal(
                    "§e" + id8 + "  §f" + c.getX() + "," + c.getY() + "," + c.getZ()
                    + "  §7pop §f" + pop + "/" + (cap == 0 ? "?" : cap)), false);
        }
        return villages.size();
    }

    // --- /minetopia give <token> ---

    private static int executeGive(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "token");

        DeferredItem<net.minecraft.world.item.Item> deferred = TOKEN_MAP.get(name);
        if (deferred == null) {
            source.sendFailure(Component.literal(
                    "Unknown token '§e" + name + "§c'. Use tab-complete to see valid names."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        player.getInventory().add(new ItemStack(deferred.get()));
        source.sendSuccess(() -> Component.literal("§aGave §f" + name + " §atoken."), false);
        return 1;
    }

    // --- /minetopia economy <villageId> ---

    private static int executeEconomy(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getLevel() instanceof ServerLevel level)) return 0;

        String idStr = StringArgumentType.getString(ctx, "villageId");

        // Accept either a full UUID or the first 8 chars shown by /minetopia list
        UUID villageId = VillageManager.get(level).getVillages().keySet().stream()
                .filter(id -> id.toString().startsWith(idStr))
                .findFirst()
                .orElse(null);

        if (villageId == null) {
            source.sendFailure(Component.literal("Village not found: §e" + idStr
                    + "§c. Use §f/minetopia list§c to see IDs."));
            return 0;
        }

        VillageEconomy economy = VillageManager.get(level).getEconomy(villageId);
        var demands = economy.getDemandSnapshot();

        if (demands.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No trade history yet for this village."), false);
            return 1;
        }

        String id8 = villageId.toString().substring(0, 8);
        source.sendSuccess(() -> Component.literal("§6=== Economy: §e" + id8 + " §6(" + demands.size() + " trades) ==="), false);
        for (var entry : demands.entrySet()) {
            int d = entry.getValue();
            String color = d > 0 ? "§c" : (d < 0 ? "§a" : "§7");
            String sign  = d > 0 ? "+" : "";
            source.sendSuccess(() -> Component.literal("  §7" + entry.getKey() + "  " + color + sign + d), false);
        }
        return demands.size();
    }

    // --- /minetopia recall ---

    private static int executeRecall(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getLevel() instanceof ServerLevel level)) return 0;

        var pos = net.minecraft.core.BlockPos.containing(source.getPosition());
        var villageOpt = VillageManager.get(level).findNearestVillage(pos);

        if (villageOpt.isEmpty()) {
            source.sendFailure(Component.literal("No Minetopia village found nearby."));
            return 0;
        }

        Village village = villageOpt.get();
        net.minecraft.core.BlockPos center = village.getCenter();
        UUID villageId = village.getId();
        int[] count = {0};

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof MinetopiaVillager villager
                    && villager.getVillageId().isPresent()
                    && villager.getVillageId().get().equals(villageId)) {
                villager.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                villager.getNavigation().stop();
                count[0]++;
            }
        }

        int recalled = count[0];
        source.sendSuccess(() -> Component.literal(
                "§aRecalled §f" + recalled + "§a villager(s) to village center."), false);
        return recalled;
    }
}
