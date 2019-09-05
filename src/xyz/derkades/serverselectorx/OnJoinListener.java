package xyz.derkades.serverselectorx;

import java.util.List;

import org.bukkit.Material;
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
			if (!putItemInInventory)
				continue;
			
			final Player player = event.getPlayer();
			
			final String itemName = config.getString("item");
			
			if (itemName.equalsIgnoreCase("none")) {
				continue;
			}
			
			Material material = Material.getMaterial(itemName);			
			
			if (material == null) {
				material = Material.STONE;
			}
			
			final ItemBuilder builder = new ItemBuilder(material)
					.coloredName(config.getString("item-name", "error"));
			
			final List<String> lore = config.getStringList("item-lore");
			
			//Don't add lore if it's null or if the first line is equal to 'none'
			if (!(lore == null || lore.isEmpty() || lore.get(0).equalsIgnoreCase("none"))) {
				builder.coloredLore(lore);
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
