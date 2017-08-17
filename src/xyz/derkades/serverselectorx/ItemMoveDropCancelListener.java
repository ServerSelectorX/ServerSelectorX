package xyz.derkades.serverselectorx;

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
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onDrop(PlayerDropItemEvent event){
		event.setCancelled(DROP_PERMISSION_ENABLED && !event.getPlayer().hasPermission("ssx.drop"));
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onItemMove(InventoryClickEvent event){
		event.setCancelled(MOVE_PERMISSION_ENABLED && !event.getWhoClicked().hasPermission("ssx.move"));
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onItemMove(InventoryDragEvent event){
		event.setCancelled(MOVE_PERMISSION_ENABLED && !event.getWhoClicked().hasPermission("ssx.move"));
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onItemMove(PlayerSwapHandItemsEvent event) {
		event.setCancelled(MOVE_PERMISSION_ENABLED && !event.getPlayer().hasPermission("ssx.move"));
	}

}
