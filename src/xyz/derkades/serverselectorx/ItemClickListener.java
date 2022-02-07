package xyz.derkades.serverselectorx;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;

import java.util.ArrayList;
import java.util.List;

public class ItemClickListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event){
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			return;
		}

		final FileConfiguration inventory = Main.getConfigurationManager().getInventoryConfiguration();

		if (event.isCancelled() && inventory.getBoolean("ignore-cancelled", false)) {
			return;
		}

		final Player player = event.getPlayer();
		final ItemStack item = player.getInventory().getItemInMainHand();

		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		// 1.16 triggers interact events when clicking items in a menu for some reason
		// We need to ignore these
		// If the player does not have an open inventory, getOpenInventory returns their crafting or creative inventory
		if (player.getOpenInventory().getType() != InventoryType.CRAFTING &&
				player.getOpenInventory().getType() != InventoryType.CREATIVE) {
			return;
		}

		// this cooldown is added when the menu closes
		if (!checkCooldown(player, "ssxitemglobal", 100, false)) {
			return;
		}

		final NBTItem nbt = new NBTItem(item);

		if (!nbt.hasKey("SSXItem")) {
			return;
		}

		final String itemName = nbt.getString("SSXItem");

		final FileConfiguration config = Main.getConfigurationManager().getItemConfiguration(itemName);

		if (config == null) {
			player.sendMessage("No configuration file exists for an item with the name '" + itemName + "'.");
			return;
		}

		final List<String> actions = new ArrayList<>(config.getStringList("actions"));

		if (config.isList("left-click-actions") &&
				(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
			actions.addAll(config.getStringList("left-click-actions"));
		}

		if (config.isList("right-click-actions") &&
				(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			actions.addAll(config.getStringList("right-click-actions"));
		}

		if (!actions.isEmpty() &&
				config.isInt("cooldown") &&
				!checkCooldown(player, "ssxitem" + itemName, config.getInt("cooldown"), true)) {
			return;
		}

		xyz.derkades.serverselectorx.actions.Action.runActions(player, actions);

		final boolean cancel = inventory.getBoolean("cancel-click-event", false);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	private boolean checkCooldown(final Player player, String id, final long duration, final boolean message) {
		id = id + player.getName();
		final long timeLeft = Cooldown.getCooldown(id);
		if (timeLeft > 0) {
			if (message && Main.getConfigurationManager().getMiscConfiguration().isString("cooldown-message")) {
				player.sendMessage(Colors.parseColors(String.format(Main.getConfigurationManager().getMiscConfiguration().getString("cooldown-message"), timeLeft / 1000.0)));
			}
			return false;
		}

		Cooldown.addCooldown(id, duration);
		return true;
	}
}
