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

	public ItemMoveDropCancelListener() {
		final FileConfiguration inventory = Main.getConfigurationManager().inventory;

		if (inventory.getBoolean("cancel-item-drop", false)) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onDrop(final PlayerDropItemEvent event){
					if (!event.getPlayer().hasPermission("ssx.drop")) {
//						event.getItemDrop().setItemStack(null);
						event.getItemDrop().remove();
						event.setCancelled(true);
					}
				}
			}, Main.getPlugin());
		}

		if (inventory.getBoolean("cancel-item-move", false)) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryClickEvent event){
					if (!event.getWhoClicked().hasPermission("ssx.move")) {
						event.setCancelled(true);
					}
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryDragEvent event){
					if (!event.getWhoClicked().hasPermission("ssx.move")) {
						event.setCancelled(true);
					}
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final PlayerSwapHandItemsEvent event) {
					if (!event.getPlayer().hasPermission("ssx.move")) {
						event.setCancelled(true);
					}
				}
			}, Main.getPlugin());
		}
	}

}
