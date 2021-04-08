package xyz.derkades.serverselectorx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;

public class ItemMoveDropCancelListener implements Listener {

	public ItemMoveDropCancelListener() {
		final FileConfiguration inventory = Main.getConfigurationManager().inventory;

		if (inventory.getBoolean("cancel-item-drop", false)) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onDrop(final PlayerDropItemEvent event){
					if (!event.getPlayer().hasPermission("ssx.drop") &&
							isSsxItem(event.getItemDrop().getItemStack())) {
						event.setCancelled(true);
						event.getItemDrop().remove();
					}
				}
			}, Main.getPlugin());
		}

		if (inventory.getBoolean("cancel-item-move", false)) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryClickEvent event){
					event.setCancelled(event.getClickedInventory() != null
							&& (!inventory.getBoolean("cancel-item-move-player-only")
									|| event.getClickedInventory().getType().equals(InventoryType.PLAYER))
							&& !event.getWhoClicked().hasPermission("ssx.move")
							&& (isSsxItem(event.getCursor())
									|| isSsxItem(event.getCurrentItem())));
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryDragEvent event){
					event.setCancelled(
							!event.getWhoClicked().hasPermission("ssx.move")
							&& isSsxItem(event.getCursor()));
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final PlayerSwapHandItemsEvent event) {
					event.setCancelled(
							!event.getPlayer().hasPermission("ssx.move")
							&& (isSsxItem(event.getMainHandItem())
									|| isSsxItem(event.getOffHandItem())));
				}
			}, Main.getPlugin());
		}
	}

	private boolean isSsxItem(final ItemStack item) {
		if (item == null) {
			return false;
		}

		if (Main.getConfigurationManager().inventory.getBoolean("ssx-items-only", false)) {
			final NBTItem nbt = new NBTItem(item);
			return nbt.hasKey("SSXItem");
		} else {
			return true;
		}
	}

}
