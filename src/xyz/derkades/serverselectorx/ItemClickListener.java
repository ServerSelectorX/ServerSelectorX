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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ItemClickListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event) {
		Logger logger = Main.getPlugin().getLogger();

		if (Main.ITEM_DEBUG) {
			logger.info("[Click debug] Player " + event.getPlayer().getName() + " performed action " + event.getAction() + " on item using " + event.getHand());
		}

		if (event.getAction() == Action.PHYSICAL) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because it was not a click event");
			}
			return;
		}

		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because it was caused by the off hand");
			}
			return;
		}

		final FileConfiguration inventory = Main.getConfigurationManager().getInventoryConfiguration();

		if (event.isCancelled() && inventory.getBoolean("ignore-cancelled", false)) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because it was cancelled and ignore-cancelled is enabled");
			}
			return;
		}

		final Player player = event.getPlayer();
		final ItemStack item = player.getInventory().getItemInMainHand();

		if (item.getType() == Material.AIR) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because the item was AIR");
			}
			return;
		}

		// 1.16 triggers interact events when clicking items in a menu for some reason
		// We need to ignore these
		// If the player does not have an open inventory, getOpenInventory returns their crafting or creative inventory
		if (player.getOpenInventory().getType() != InventoryType.CRAFTING &&
				player.getOpenInventory().getType() != InventoryType.CREATIVE) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because the player had an open inventory: " + player.getOpenInventory().getType());
			}
			return;
		}

		final NBTItem nbt = new NBTItem(item);

		if (!nbt.hasKey("SSXActions")) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because the clicked item is not an SSX item");
			}
			// Not an SSX item
			return;
		}

		// this cooldown is added when the menu closes
		String globalCooldownId = player.getName() + "ssxitemglobal";
		if (Cooldown.getCooldown(globalCooldownId) > 0) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because the global cooldown was in effect (this cooldown cannot be disabled)");
			}
			return;
		}
		Cooldown.addCooldown(globalCooldownId, 100);

		List<String> actions = new ArrayList<>(nbt.getStringList("SSXActions"));
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			actions.addAll(nbt.getStringList("SSXActionsLeft"));
		}
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			actions.addAll(nbt.getStringList("SSXActionsRight"));
		}

		if (actions.isEmpty()) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] Event was ignored because the item has no actions");
			}
			return;
		}

		if (nbt.hasKey("SSXCooldownId")) {
			String cooldownId = nbt.getString("SSXCooldownId");
			if (Cooldown.getCooldown(cooldownId) > 0) {
				List<String> cooldownActions = nbt.getStringList("SSXCooldownActions");
				xyz.derkades.serverselectorx.actions.Action.runActions(player, cooldownActions);
				if (Main.ITEM_DEBUG) {
					logger.info("[Click debug] Event was ignored because the configured cooldown was in effect");
				}
				return;
			}
			int cooldownTime = nbt.getInteger("SSXCooldownTime");
			Cooldown.addCooldown(cooldownId, cooldownTime);
		}

		xyz.derkades.serverselectorx.actions.Action.runActions(player, actions);

		if (Main.ITEM_DEBUG) {
			logger.info("[Click debug] Event handling completed, actions have been run.");
		}

		final boolean cancel = inventory.getBoolean("cancel-click-event", false);
		if (cancel) {
			if (Main.ITEM_DEBUG) {
				logger.info("[Click debug] The event has been cancelled, because cancel-click-event is enabled");
			}
			event.setCancelled(true);
		}
	}

}
