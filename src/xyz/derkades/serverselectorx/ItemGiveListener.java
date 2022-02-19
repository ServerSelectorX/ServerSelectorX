package xyz.derkades.serverselectorx;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xyz.derkades.derkutils.bukkit.NbtItemBuilder;
import xyz.derkades.serverselectorx.conditional.ConditionalItem;

public class ItemGiveListener implements Listener {

	// Event priorities are set to high so the items are given after other plugins clear the player's inventory.

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final FileConfiguration config = Main.getConfigurationManager().getInventoryConfiguration();

		if (config.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			debug("Clearning inventory for " + player.getName());
			final PlayerInventory inv = player.getInventory();
			inv.setStorageContents(new ItemStack[inv.getStorageContents().length]);
		}

		this.giveItems(player, "join");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onWorldChange(final PlayerChangedWorldEvent event) {
		this.giveItems(event.getPlayer(), "world-switch");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(final PlayerRespawnEvent event) {
		this.giveItems(event.getPlayer(), "death");
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onClear(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().equals("/clear") && event.getPlayer().hasPermission("minecraft.command.clear")) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> this.giveItems(event.getPlayer(), "clear"), 1);
		}
	}

	public void giveItems(final Player player, final String type) {
		debug("Giving items to " + player.getName() + ". Reason: " + type);

		debug("First removing any existing SSX items");

		final PlayerInventory inv = player.getInventory();
		final ItemStack[] contents = inv.getStorageContents();
		for (int i = 0; i < contents.length; i++) {
			final ItemStack item = contents[i];
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
				final NBTItem nbt = new NBTItem(item);
			if (nbt.hasKey("SSXActions")) {
				debug("Removed item at position " + i);
				contents[i] = null;
			}
		}
		inv.setStorageContents(contents);

		debug("Now giving items");

		for (final String configName : Main.getConfigurationManager().listItemConfigurations()) {
			final FileConfiguration config = Main.getConfigurationManager().getItemConfiguration(configName);

			debug("Preparing to give item '" + configName + "'");

			if (!config.getBoolean("give." + type)) {
				debug("Item skipped, give is disabled");
				continue;
			}

			if (config.getBoolean("give.permission")) {
				debug("Permissions are enabled, checking permission");
				final String permission = "ssx.item." + configName;
				if (!player.hasPermission(permission)) {
					debug("Player does not have permission '" + permission + "', skipping item");
					continue;
				}
			}

			if (config.isList("worlds")) {
				debug("World whitelisting is enabled");
				// World whitelisting option is present
				if (!config.getStringList("worlds").contains(player.getWorld().getName())) {
					debug("Player is in a world that is not whitelisted (" + player.getWorld().getName() + ")");
					continue;
				}
			}

			debug("All checks done, giving item");

			if (!config.isConfigurationSection("item")) {
				player.sendMessage("Missing 'item' section in item config '" + configName + "'");
				continue;
			}

			String cooldownId = player.getUniqueId() + configName;

			try {
				ConditionalItem.getItem(player, config.getConfigurationSection("item"), cooldownId, itemBeforeModify -> {
					final int slot = config.getInt("give.inv-slot", 0);
					final int delay = config.getInt("give.delay", 0);
					debug("Give delay: " + delay);

					if (slot < 0) {
						if (!inv.containsAtLeast(item, item.getAmount())) {
							if (delay == 0) {
								inv.addItem(item);
							} else {
								final ItemStack itemF = item;
								Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> inv.addItem(itemF), delay);
							}
						}
					} else {
						if (delay == 0) {
							inv.setItem(slot, item);
						} else {
							final ItemStack itemF = item;
							Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> inv.setItem(slot, itemF), delay);
						}
					}
				});
			} catch (InvalidConfigurationException e) {
				player.sendMessage(String.format("Invalid item config (in %s.yaml): %s",
						configName, e.getMessage()));
			}
		}
	}

	private void debug(final String message) {
		if (Main.ITEM_DEBUG) {
			Main.getPlugin().getLogger().info("[item debug] " + message);
		}
	}

}
