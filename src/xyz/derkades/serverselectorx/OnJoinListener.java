package xyz.derkades.serverselectorx;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xyz.derkades.derkutils.bukkit.ItemBuilder;

public class OnJoinListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(final PlayerJoinEvent event) {
		for (final FileConfiguration config : Main.getConfigurationManager().allFiles()) {
			final boolean putItemInInventory = config.getBoolean("on-join");
			if (!putItemInInventory) {
				continue;
			}

			final Player player = event.getPlayer();

			if (config.contains("only-in-worlds") &&
					!config.getStringList("only-in-worlds").contains(player.getWorld().getName())) {
				return;
			}

			final String materialString = config.getString("item");

			final ItemBuilder builder = Main.getItemFromMaterialString(player, materialString)
					.coloredName(config.getString("item-name", "error"));

			builder.coloredLore(config.getStringList("item-lore"));

			final int slot = config.getInt("inv-slot", 0);
			final PlayerInventory inv = player.getInventory();
			final ItemStack item = builder.create();
			if (slot < 0) {
				if (!inv.contains(item.getType())) {
					inv.addItem(item);
				}
			} else {
				inv.setItem(slot, item);
			}

		}
	}

}
