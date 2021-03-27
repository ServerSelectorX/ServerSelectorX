package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;

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

import de.tr7zw.changeme.nbtapi.NBTItem;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;

public class ItemClickListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event){
		event.getPlayer().sendMessage("Debug - action: " + event.getAction());
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		event.getPlayer().sendMessage("Debug - hand: " + event.getHand());

		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			return;
		}

		final FileConfiguration inventory = Main.getConfigurationManager().inventory;

		if (event.isCancelled() && inventory.getBoolean("ignore-cancelled", false)) {
			return;
		}

		final Player player = event.getPlayer();
		final ItemStack item = player.getInventory().getItemInMainHand();

		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		player.sendMessage("Debug - open inventory: " + player.getOpenInventory().getType());

		// 1.16 triggers interact events when clicking items in a menu for some reason
		// We need to ignore these
		// If the player does not have an open inventory, getOpenInventory returns their crafting inventory
		if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
			return;
		}

		final NBTItem nbt = new NBTItem(item);

		if (!nbt.hasKey("SSXItem")) {
			return;
		}

		final String itemName = nbt.getString("SSXItem");

		if (!Main.getConfigurationManager().items.containsKey(itemName)) {
			player.sendMessage("No configuration file exists for an item with the name '" + itemName + "'.");
			return;
		}

		final FileConfiguration config = Main.getConfigurationManager().items.get(itemName);

		if (config.isInt("cooldown")) {
			final long timeLeft = Cooldown.getCooldown("ssxitem" + itemName);
			if (timeLeft > 0) {
				player.sendMessage(Colors.parseColors(String.format(Main.getConfigurationManager().misc.getString("cooldown-message"), timeLeft / 1000.0)));
				return;
			}

			Cooldown.addCooldown("ssxitem" + itemName, config.getInt("cooldown"));
		}

		final List<String> actions = new ArrayList<>();

		actions.addAll(config.getStringList("actions"));

		if (config.isList("left-click-actions") &&
				(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
			actions.addAll(config.getStringList("left-click-actions"));
		}

		if (config.isList("right-click-actions") &&
				(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			actions.addAll(config.getStringList("right-click-actions"));
		}

		xyz.derkades.serverselectorx.actions.Action.runActions(player, actions);

		final boolean cancel = inventory.getBoolean("cancel-click-event", false);
		if (cancel) {
			event.setCancelled(true);
		}
	}
}
