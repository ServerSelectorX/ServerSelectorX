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

public class OnJoinListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final PlayerJoinEvent event) {
		if (Main.getConfigurationManager().getSSXConfig().getBoolean("disable-items"))
			return;

		final FileConfiguration global = Main.getConfigurationManager().getGlobalConfig();

		final Player player = event.getPlayer();

		if (global.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			event.getPlayer().getInventory().clear();
		}

		menusLoop: for (final Map.Entry<String, FileConfiguration> menuConfigEntry :
			Main.getConfigurationManager().getAllMenus().entrySet()) {

			final FileConfiguration menuConfig = menuConfigEntry.getValue();
			final String configName = menuConfigEntry.getKey();

			if (!menuConfig.getBoolean("item.enabled"))
			 {
				continue; // Item is disabled
			}

			if (!menuConfig.getBoolean("item.on-join.enabled"))
			 {
				continue; // Item on join is disbled
			}

			if (menuConfig.contains("item.worlds")) {
				// World whitelisting option is present
				for (final String worldName : menuConfig.getStringList("item.worlds")) {
					if (!player.getWorld().getName().equalsIgnoreCase(worldName)) {
						continue menusLoop;
					}
				}
			}

			if (menuConfig.getBoolean("permission.item")) {
				if (!player.hasPermission("ssx.item." + configName)) {
					final String permission = "ssx.item." + configName;
					if (!player.hasPermission(permission)) {
						if (menuConfig.getBoolean("permission.debug")) {
							Main.getPlugin().getLogger().info(String.format("%s did not receive an item because they don't have the permission %s",
									player.getName(), permission));
						}
						continue;
					}
				}
			}

			final ItemStack item = Main.getHotbarItemStackFromMenuConfig(player, menuConfig, configName);

			final int slot = menuConfig.getInt("item.on-join.inv-slot", 0);
			final PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(item, 1)) {
					inv.addItem(item);
				}
			} else {
				inv.setItem(slot, item);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void applyEffects(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final FileConfiguration global = Main.getConfigurationManager().getGlobalConfig();

		if (global.getBoolean("hide-others-on-join", false)) {
			InvisibilityToggle.hideOthers(player);
		}
		
		new EffectsTimer(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void sendAnnoyingMessage(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (player.hasPermission("ssx.reload") && Main.BETA) {
			player.sendMessage("You are using a beta version of ServerSelectorX. Please update to a stable version as soon as the functionality or bugfix you require is available in a stable release version. " + ChatColor.GRAY + "(only players with the ssx.reload permission will see this message)");
		}
	}

}
