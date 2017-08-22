package xyz.derkades.serverselectorx;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class OffHandMoveCancel implements Listener {
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onItemMove(PlayerSwapHandItemsEvent event) {
		event.setCancelled(ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED && !event.getPlayer().hasPermission("ssx.move"));
	}

}
