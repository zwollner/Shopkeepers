package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityAI;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandCheck extends Command {

	private static final String ARGUMENT_CHUNKS = "chunks";
	private static final String ARGUMENT_ACTIVE = "active";

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;

	CommandCheck(SKShopkeepersPlugin plugin) {
		super("check");
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();

		// set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// set description:
		this.setDescription(Text.of("Shows various debugging information."));

		// hidden debugging command:
		this.setHiddenInParentHelp(true);

		// arguments:
		this.addArgument(new OptionalArgument<>(new FirstOfArgument("context", Arrays.asList(
				new LiteralArgument(ARGUMENT_CHUNKS),
				new LiteralArgument(ARGUMENT_ACTIVE)
		), true))); // join formats
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		boolean isConsole = (sender instanceof ConsoleCommandSender);

		boolean listChunks = context.has(ARGUMENT_CHUNKS);
		boolean listActive = context.has(ARGUMENT_ACTIVE);

		LivingEntityAI livingEntityAI = plugin.getLivingShops().getLivingEntityAI();

		int totalChunksWithShopkeepers = shopkeeperRegistry.getWorldsWithShopkeepers().stream()
				.map(worldName -> shopkeeperRegistry.getShopkeepersByChunks(worldName))
				.mapToInt(byChunk -> byChunk.size())
				.sum();

		sender.sendMessage(ChatColor.YELLOW + "All shopkeepers:");
		sender.sendMessage("  Total: " + shopkeeperRegistry.getAllShopkeepers().size()
				+ "    (Virtual: " + shopkeeperRegistry.getVirtualShopkeepers().size() + ")");
		sender.sendMessage("  Unsaved dirty | deleted | dirty storage: "
				+ plugin.getShopkeeperStorage().getDirtyCount()
				+ " | " + plugin.getShopkeeperStorage().getUnsavedDeletedCount()
				+ " | " + plugin.getShopkeeperStorage().isDirty());
		sender.sendMessage("  Chunks with shopkeepers: " + totalChunksWithShopkeepers);
		sender.sendMessage("    With active AI: " + livingEntityAI.getActiveAIChunksCount());
		sender.sendMessage("    With active gravity: " + livingEntityAI.getActiveGravityChunksCount());
		sender.sendMessage("  Active shopkeepers: " + shopkeeperRegistry.getActiveShopkeepers().size());
		sender.sendMessage("    With AI: " + livingEntityAI.getEntityCount());
		sender.sendMessage("    With active AI: " + livingEntityAI.getActiveAIEntityCount());
		sender.sendMessage("    With active gravity: " + livingEntityAI.getActiveGravityEntityCount());

		double avgTotalAITimings = livingEntityAI.getTotalTimings().getAverageTimeMillis();
		double maxTotalAITiming = livingEntityAI.getTotalTimings().getMaxTimeMillis();
		sender.sendMessage("  Total AI timings (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgTotalAITimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxTotalAITiming) + " ms");

		// note: these are per activation, which happens only every 20 ticks (not per tick)
		double avgAIActivationTimings = livingEntityAI.getActivationTimings().getAverageTimeMillis();
		double maxAIActivationTiming = livingEntityAI.getActivationTimings().getMaxTimeMillis();
		sender.sendMessage("    AI activation timings (per " + LivingEntityAI.AI_ACTIVATION_TICK_RATE + " ticks) (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgAIActivationTimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxAIActivationTiming) + " ms");

		double avgGravityTimings = livingEntityAI.getGravityTimings().getAverageTimeMillis();
		double maxGravityTiming = livingEntityAI.getGravityTimings().getMaxTimeMillis();
		sender.sendMessage("    Gravity timings (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgGravityTimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxGravityTiming) + " ms");

		double avgAITimings = livingEntityAI.getAITimings().getAverageTimeMillis();
		double maxAITiming = livingEntityAI.getAITimings().getMaxTimeMillis();
		sender.sendMessage("    AI timings (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgAITimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxAITiming) + " ms");

		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName();
			Chunk[] worldLoadedChunks = world.getLoadedChunks();
			List<Entity> worldEntities = world.getEntities();
			int worldDeadEntities = 0;
			int worldInvalidEntities = 0;
			for (Entity entity : worldEntities) {
				if (entity.isDead()) ++worldDeadEntities;
				if (!entity.isValid()) ++worldInvalidEntities;
			}
			int worldDeadEntitiesInChunks = 0;
			int worldInvalidEntitiesInChunks = 0;
			for (Chunk chunk : worldLoadedChunks) {
				for (Entity entity : chunk.getEntities()) {
					if (entity.isDead()) ++worldDeadEntitiesInChunks;
					if (!entity.isValid()) ++worldInvalidEntitiesInChunks;
				}
			}

			int worldTotalShopkeepers = shopkeeperRegistry.getShopkeepersInWorld(worldName).size();
			int worldActiveChunks = shopkeeperRegistry.getActiveChunks(worldName).size();
			int worldShopkeepersInActiveChunks = shopkeeperRegistry.getShopkeepersInActiveChunks(worldName).size();

			int worldChunksWithShopkeepers = 0;
			int worldLoadedChunksWithShopkeepers = 0;
			int worldShopkeepersInLoadedChunks = 0;

			Map<ChunkCoords, ? extends Collection<? extends Shopkeeper>> shopkeepersByChunks = shopkeeperRegistry.getShopkeepersByChunks(worldName);
			for (Entry<ChunkCoords, ? extends Collection<? extends Shopkeeper>> chunkEntry : shopkeepersByChunks.entrySet()) {
				ChunkCoords chunkCoords = chunkEntry.getKey();
				Collection<? extends Shopkeeper> chunkShopkeepers = chunkEntry.getValue();

				worldChunksWithShopkeepers++;
				if (chunkCoords.isChunkLoaded()) {
					worldLoadedChunksWithShopkeepers++;
					worldShopkeepersInLoadedChunks += chunkShopkeepers.size();
				}

			}

			sender.sendMessage(ChatColor.YELLOW + "Shopkeepers in world '" + world.getName() + "':");
			sender.sendMessage("  Total: " + worldTotalShopkeepers);
			sender.sendMessage("  Entities | invalid | dead: " + worldEntities.size() + " | " + worldInvalidEntities + " | " + worldDeadEntities);
			sender.sendMessage("  Entities in chunks (invalid | dead): " + worldInvalidEntitiesInChunks + " | " + worldDeadEntitiesInChunks);
			sender.sendMessage("  Loaded chunks: " + worldLoadedChunks.length);
			if (worldTotalShopkeepers > 0) {
				sender.sendMessage("  Chunks with shopkeepers | loaded | active: " + worldChunksWithShopkeepers + " | " + worldLoadedChunksWithShopkeepers + " | " + worldActiveChunks);
				sender.sendMessage("  Shopkeepers in chunks (loaded | active): " + worldShopkeepersInLoadedChunks + " | " + worldShopkeepersInActiveChunks);
			}

			// list all chunks containing shopkeepers:
			if (isConsole && listChunks && worldTotalShopkeepers > 0) {
				sender.sendMessage("  Listing of all chunks with shopkeepers:");
				int line = 0;
				for (Entry<ChunkCoords, ? extends Collection<? extends Shopkeeper>> chunkEntry : shopkeepersByChunks.entrySet()) {
					ChunkCoords chunkCoords = chunkEntry.getKey();
					Collection<? extends Shopkeeper> chunkShopkeepers = chunkEntry.getValue();

					line++;
					ChatColor lineColor = (line % 2 == 0 ? ChatColor.WHITE : ChatColor.GRAY);
					sender.sendMessage("    (" + lineColor + chunkCoords.getChunkX() + "," + chunkCoords.getChunkZ() + ChatColor.RESET + ") ["
							+ (chunkCoords.isChunkLoaded() ? ChatColor.GREEN + "loaded" : ChatColor.DARK_GRAY + "unloaded")
							+ ChatColor.RESET + " | "
							+ (shopkeeperRegistry.isChunkActive(chunkCoords) ? ChatColor.GREEN + "active" : ChatColor.DARK_GRAY + "inactive")
							+ ChatColor.RESET + "]: " + chunkShopkeepers.size());
				}
			}
		}

		// list all active shopkeepers:
		if (isConsole && listActive) {
			sender.sendMessage("All active shopkeepers:");
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getActiveShopkeepers()) {
				ShopObject shopObject = shopkeeper.getShopObject();
				if (shopObject.isActive()) {
					Location location = shopObject.getLocation();
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": active (" + (location != null ? location.toString() : "maybe not?!?") + ")");
				} else {
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": INACTIVE!");
				}
			}
		}

		if (!isConsole && (listChunks || listActive)) {
			sender.sendMessage("There might be more information getting printed if the command is run from the console.");
		}
	}
}
