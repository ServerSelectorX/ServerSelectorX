package xyz.derkades.serverselectorx;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.tr7zw.nbtapi.NBTItem;

public class OnJoinListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
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

		itemLoop:
		for (final Map.Entry<String, FileConfiguration> itemConfigEntry : Main.getConfigurationManager().getItems().entrySet()) {
			final String name = itemConfigEntry.getKey();
			final FileConfiguration config = itemConfigEntry.getValue();

			if (!config.getBoolean("give.join")) {
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

			final int slot = config.getInt("on-join.inv-slot", 0);
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

	@EventHandler(priority = EventPriority.MONITOR)
	public void sendAnnoyingMessage(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (player.hasPermission("ssx.reload") && Main.BETA) {
			player.sendMessage("You are using a beta version of ServerSelectorX. Please update to a stable version as soon as the functionality or bugfix you require is available in a stable release version. " + ChatColor.GRAY + "(only players with the ssx.reload permission will see this message)");
		}
	}

}
