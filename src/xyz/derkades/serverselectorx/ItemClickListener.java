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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;

public class ItemClickListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event){
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		final FileConfiguration inventory = Main.getConfigurationManager().getInventoryConfiguration();

		if (event.isCancelled() && inventory.getBoolean("ignore-cancelled", false)) {
			return;
		}

		final Player player = event.getPlayer();
		final ItemStack item = player.getInventory().getItemInHand();

		if (item == null || item.getType() == Material.AIR) {
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

		if (config.isInt("cooldown")) {
			final long timeLeft = Cooldown.getCooldown("ssxitem" + itemName);
			if (timeLeft > 0) {
				player.sendMessage(Colors.parseColors(String.format(Main.getConfigurationManager().getMiscConfiguration().getString("cooldown-message"), timeLeft / 1000.0)));
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

		/*
		 * On 1.9-1.12, this event is sometimes called twice (once for each hand). Bukkit
		 * has a proper method of checking which hand is used, but since this version is
		 * compiled against 1.8 I can't use that here. Unless I use reflection, which I am
		 * not going to, because I'm lazy. A cooldown of 200ms does the same thing.
		 */
		if (Cooldown.getCooldown("ssxclick" + player.getName()) > 0) {
			return;
		}
		Cooldown.addCooldown("ssxclick" + player.getName(), 200);

		xyz.derkades.serverselectorx.actions.Action.runActions(player, actions);

		final boolean cancel = inventory.getBoolean("cancel-click-event", false);
		if (cancel) {
			event.setCancelled(true);
		}
	}
}
