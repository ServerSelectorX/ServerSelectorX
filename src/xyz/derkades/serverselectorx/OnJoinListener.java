package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.GRAY;

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
	public void onJoin(PlayerJoinEvent event) {
		for (FileConfiguration config : Main.getConfigurationManager().getAll()) {			
			boolean putItemInInventory = config.getBoolean("on-join");
			if (!putItemInInventory)
				continue;
			
			Player player = event.getPlayer();
			
			if (Main.getPlugin().getConfig().getBoolean("permissions-enabled")) {
				if (!player.hasPermission("ssx.join." + config.getName().replace(".yml", ""))) {
					continue;
				}
			}
			
			String itemName = config.getString("item");
			
			if (itemName.equalsIgnoreCase("none")) {
				continue;
			}
			
			Material material = Material.getMaterial(itemName);			
			
			if (material == null) {
				material = Material.STONE;
			}
			
			ItemBuilder builder = new ItemBuilder(material)
					.data(config.getInt("data", 0))
					.coloredName(config.getString("item-name", "error"));
			
			List<String> lore = config.getStringList("item-lore");
			
			//Don't add lore if it's null or if the first line is equal to 'none'
			if (!(lore == null || lore.isEmpty() || lore.get(0).equalsIgnoreCase("none"))) {
				builder.coloredLore(lore);
			}
			
			int slot = config.getInt("inv-slot", 0);
			PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(builder.create(), 1)) {
					inv.addItem(builder.create());
				}
			} else {
				inv.setItem(slot, builder.create());
			}
			
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void sendUpdateMessage(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		if (Main.UPDATE_AVAILABLE && player.hasPermission("ssx.update")) {
			player.sendMessage(AQUA + "An update is available for ServerSelectorX.");
			player.sendMessage(String.format(AQUA + "Latest version: %s " + GRAY + "(you are now using version %s)", Main.NEW_VERSION, Main.CURRENT_VERSION)); 
			player.sendMessage(AQUA + "Read the changelog here: " + GRAY + Main.DOWNLOAD_LINK);
		}
	}

}
