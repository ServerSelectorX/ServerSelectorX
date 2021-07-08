package xyz.derkades.serverselectorx;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;

public class ItemMoveDropCancelListener implements Listener {

	public ItemMoveDropCancelListener() {
		final FileConfiguration inventory = Main.getConfigurationManager().getInventoryConfiguration();

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
				public void onItemMove(final InventoryClickEvent event) {
					event.setCancelled(
						event.getClickedInventory() != null &&
						!event.getWhoClicked().hasPermission("ssx.move") &&
						(
							isSsxItem(event.getCursor()) ||
							isSsxItem(event.getCurrentItem()) ||
							(
								event.getClick() == ClickType.NUMBER_KEY &&
								isSsxItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))
							)
						)
					);
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
				public void onItemMove(final InventoryDragEvent event){
					event.setCancelled(
							!event.getWhoClicked().hasPermission("ssx.move")
							&& isSsxItem(event.getCursor()));
				}
			}, Main.getPlugin());
		}
	}

	private boolean isSsxItem(final ItemStack item) {
		if (!Main.getConfigurationManager().getInventoryConfiguration().getBoolean("ssx-items-only", false)) {
			return true;
		}

		if (item == null || item.getType() == Material.AIR) {
			return false;
		} else {
			final NBTItem nbt = new NBTItem(item);
			return nbt.hasKey("SSXItem");
		}
	}

}
