package xyz.derkades.serverselectorx;

import java.util.Map;

import org.bukkit.Bukkit;
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

import de.tr7zw.nbtapi.NBTItem;

public class GiveItemsListener implements Listener {

	// Event priorities are set to high so the items are given after other plugins clear the player's inventory.

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final FileConfiguration global = Main.getConfigurationManager().getGlobalConfig();

		if (global.getBoolean("hide-others-on-join", false)) {
			InvisibilityToggle.hideOthers(player);
		}

		final FileConfiguration ssx = Main.getConfigurationManager().getSSXConfig();

		if (ssx.getBoolean("config-sync.enabled") && ssx.getBoolean("config-sync.disable-items"))
			return;

		if (global.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			event.getPlayer().getInventory().clear();
		}

		giveItems(player, "join");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onWorldChange(final PlayerChangedWorldEvent event) {
		giveItems(event.getPlayer(), "world-switch");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(final PlayerRespawnEvent event) {
		giveItems(event.getPlayer(), "death");
	}

	@EventHandler
	public void onClear(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().equalsIgnoreCase("/clear")) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> giveItems(event.getPlayer(), "clear"), 1);
		}
	}

	public void giveItems(final Player player, final String type) {
		itemLoop:
		for (final Map.Entry<String, FileConfiguration> itemConfigEntry : Main.getConfigurationManager().getItems().entrySet()) {
			final String name = itemConfigEntry.getKey();
			final FileConfiguration config = itemConfigEntry.getValue();

			if (!config.getBoolean("give." + type)) {
				continue;
			}

			if (config.getBoolean("give.permission")) {
				final String permission = "ssx.item." + name;
				if (!player.hasPermission(permission)) {
					if (config.getBoolean("permission.debug")) {
						Main.getPlugin().getLogger().info(String.format("%s did not receive an item because they don't have the permission %s",
								player.getName(), permission));
					}
					continue;
				}
			}

			if (config.contains("worlds")) {
				// World whitelisting option is present
				for (final String worldName : config.getStringList("worlds")) {
					if (!player.getWorld().getName().equalsIgnoreCase(worldName)) {
						continue itemLoop;
					}
				}
			}

			ItemStack item = Main.getItemBuilderFromItemSection(player, config.getConfigurationSection("item")).create();

			final NBTItem nbt = new NBTItem(item);
			nbt.setString("SSXItem", name);
			item = nbt.getItem();

			final int slot = config.getInt("give.inv-slot", 0);
			final PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(item, item.getAmount())) {
					inv.addItem(item);
				}
			} else {
				inv.setItem(slot, item);
			}
		}
	}

}
