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
		for (FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {			
			boolean putItemInInventory = config.getBoolean("on-join");
			if (!putItemInInventory)
				continue;
			
			Player player = event.getPlayer();
			Material material = Material.getMaterial(config.getString("item"));
			
			if (material == null) {
				player.sendMessage(Message.INVALID_ITEM_NAME.toString());
				return;
			}
			
			ItemBuilder builder = new ItemBuilder(material)
					.coloredName(config.getString("item-name", "error"));
			
			List<String> lore = config.getStringList("item-lore");
			
			//Don't add lore if it's null or if the first line is equal to 'none'
			if (!(lore == null || lore.get(0).equalsIgnoreCase("none"))) {
				builder.coloredLore(lore);
			}
			
			int slot = config.getInt("inv-slot");
			PlayerInventory inv = player.getInventory();
			
			inv.setItem(slot, builder.create());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void sendUpdateMessage(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		if (!player.hasPermission("ssx.update")) {
			return;
		}
		
		if (Main.UPDATE_AVAILABLE) {
			player.sendMessage(AQUA + "An update is available.");
			player.sendMessage(String.format(AQUA + "Latest version: %s " + GRAY + "(you are now using version %s)", Main.NEW_VERSION, Main.CURRENT_VERSION)); 
			player.sendMessage(AQUA + "Download here: " + GRAY + Main.DOWNLOAD_LINK);
		}
	}

}
