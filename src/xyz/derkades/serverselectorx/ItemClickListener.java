package xyz.derkades.serverselectorx;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import xyz.derkades.derkutils.Cooldown;

import java.util.ArrayList;
import java.util.List;

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

		if (item.getType() == Material.AIR) {
			return;
		}

		final NBTItem nbt = new NBTItem(item);

		if (!nbt.hasKey("SSXActions")) {
			// Not an SSX item
			return;
		}

		// this cooldown is added when the menu closes
		String globalCooldownId = player.getName() + "ssxitemglobal";
		if (Cooldown.getCooldown(globalCooldownId) > 0) {
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
			return;
		}

		if (nbt.hasKey("SSXCooldownId")) {
			String cooldownId = nbt.getString("SSXCooldownId");
			if (Cooldown.getCooldown(cooldownId) > 0) {
				List<String> cooldownActions = nbt.getStringList("SSXCooldownActions");
				xyz.derkades.serverselectorx.actions.Action.runActions(player, cooldownActions);
				return;
			}
			int cooldownTime = nbt.getInteger("SSXCooldownTime");
			Cooldown.addCooldown(cooldownId, cooldownTime);
		}

		xyz.derkades.serverselectorx.actions.Action.runActions(player, actions);

		final boolean cancel = inventory.getBoolean("cancel-click-event", false);
		if (cancel) {
			event.setCancelled(true);
		}
	}

}
