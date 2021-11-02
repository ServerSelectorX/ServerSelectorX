package xyz.derkades.serverselectorx;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemMoveDropCancelListener implements Listener {

	public static boolean DROP_PERMISSION_ENABLED = false;
	public static boolean MOVE_PERMISSION_ENABLED = false;

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onDrop(final PlayerDropItemEvent event){
		if (DROP_PERMISSION_ENABLED && !event.getPlayer().hasPermission("ssx.drop")) {
			event.getItemDrop().remove();
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onItemMove(final InventoryClickEvent event) {
		event.setCancelled(
				MOVE_PERMISSION_ENABLED &&
				!event.getWhoClicked().hasPermission("ssx.move") &&
				event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.PLAYER
		);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onItemMove(final InventoryDragEvent event){
		event.setCancelled(
				MOVE_PERMISSION_ENABLED &&
				!event.getWhoClicked().hasPermission("ssx.move") &&
				event.getInventory().getType() == InventoryType.PLAYER
		);
	}



}
