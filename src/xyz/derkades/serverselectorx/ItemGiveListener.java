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

import de.tr7zw.changeme.nbtapi.NBTItem;

public class ItemGiveListener implements Listener {

	// Event priorities are set to high so the items are given after other plugins clear the player's inventory.

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final FileConfiguration config = Main.getConfigurationManager().inventory;

		if (config.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			event.getPlayer().getInventory().clear();
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

	@EventHandler
	public void onClear(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().equalsIgnoreCase("/clear")) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> this.giveItems(event.getPlayer(), "clear"), 1);
		}
	}

	public void giveItems(final Player player, final String type) {
		itemLoop:
		for (final Map.Entry<String, FileConfiguration> itemConfigEntry : Main.getConfigurationManager().items.entrySet()) {
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
				if (!config.getStringList("worlds").contains(player.getWorld().getName())) {
					continue itemLoop;
				}
			}

			ItemStack item = Main.getItemBuilderFromItemSection(player, config.getConfigurationSection("item")).create();

			final NBTItem nbt = new NBTItem(item);
			nbt.setString("SSXItem", name);
			item = nbt.getItem();

			final int slot = config.getInt("give.inv-slot", 0);
			final int delay = config.getInt("give.delay", 0);
			final PlayerInventory inv = player.getInventory();
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
		}
	}

}
