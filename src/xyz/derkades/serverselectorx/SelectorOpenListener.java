package xyz.derkades.serverselectorx;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import xyz.derkades.derkutils.Cooldown;

public class SelectorOpenListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event){
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		if (!event.getHand().equals(EquipmentSlot.HAND)) {
			return;
		}


		final Player player = event.getPlayer();

		if (Cooldown.getCooldown("ssx-global-open" + player.getName()) > 0) {
			return;
		}

		Cooldown.addCooldown("ssx-global-open" + player.getName(), 300);

		for (final FileConfiguration config : Main.getConfigurationManager().allFiles()) {
			if (config == null) {
				continue;
			}

			if (!config.isString("item")) {
				continue;
			}

			if (config.getString("item").equalsIgnoreCase("NONE")){
				continue;
			}

			final String string = config.getString("item");
			Material material = Material.getMaterial(string);

			if (material == null){
				material = Material.STONE;
			}

			if (player.getInventory().getItemInMainHand().getType() != material){
				continue;
			}

			if (!SelectorMenu.checkPermission(player, config)) {
				return;
			}

			new SelectorMenu(player, config);
			return;
		}
	}

}
