package xyz.derkades.serverselectorx;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.PlayerInventory;

import xyz.derkades.derkutils.bukkit.ItemBuilder;

public class OnJoinListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(final PlayerJoinEvent event) {
		for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
			final boolean putItemInInventory = config.getBoolean("on-join");
			if (!putItemInInventory) {
				continue;
			}

			final Player player = event.getPlayer();

			final String materialString = config.getString("item");

			final ItemBuilder builder = Main.getItemFromMaterialString(player, materialString)
					.coloredName(config.getString("item-name", "error"));

			builder.coloredLore(config.getStringList("item-lore"));

			final int data = config.getInt("data");
			if (data != 0) {
				builder.damage(data);
			}

			final int slot = config.getInt("inv-slot", 0);
			final PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(builder.create(), 1)) {
					inv.addItem(builder.create());
				}
			} else {
				inv.setItem(slot, builder.create());
			}

		}
	}

}
