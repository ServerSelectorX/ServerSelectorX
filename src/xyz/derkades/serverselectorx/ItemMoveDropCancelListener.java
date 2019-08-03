package xyz.derkades.serverselectorx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class ItemMoveDropCancelListener implements Listener {
	
	public static boolean DROP_PERMISSION_ENABLED = false;
	public static boolean MOVE_PERMISSION_ENABLED = false;
	
	public ItemMoveDropCancelListener() {
		final FileConfiguration global = Main.getConfigurationManager().getGlobalConfig();
		if (global.getBoolean("cancel-item-drop", false)) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onDrop(final PlayerDropItemEvent event){
					event.setCancelled(DROP_PERMISSION_ENABLED && !event.getPlayer().hasPermission("ssx.drop"));
				}
			}, Main.getPlugin());
		}
		
		if (global.getBoolean("cancel-item-move", false)) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryClickEvent event){
					event.setCancelled(MOVE_PERMISSION_ENABLED && !event.getWhoClicked().hasPermission("ssx.move"));
				}
				
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryDragEvent event){
					event.setCancelled(MOVE_PERMISSION_ENABLED && !event.getWhoClicked().hasPermission("ssx.move"));
				}
				
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final PlayerSwapHandItemsEvent event) {
					event.setCancelled(MOVE_PERMISSION_ENABLED && !event.getPlayer().hasPermission("ssx.move"));
				}
			}, Main.getPlugin());
		}
	}

}
