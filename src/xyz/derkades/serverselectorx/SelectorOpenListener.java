package xyz.derkades.serverselectorx;

import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import xyz.derkades.derkutils.Cooldown;

public class SelectorOpenListener implements Listener {

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event){
		final FileConfiguration globalConfig = Main.getConfigurationManager().getGlobalConfig();
		
		if (event.isCancelled() && globalConfig.getBoolean("ignore-cancelled", false)) {
			return;
		}
		
		final boolean openOnRightClick = globalConfig.getBoolean("right-click-open", true);
		final boolean openOnLeftClick = globalConfig.getBoolean("left-click-open", false);
		
		if (!(
				openOnRightClick && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) ||
				openOnLeftClick && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
				)) return;
		
		Player player = event.getPlayer();
		
		//Small cooldown because on 1.9+ interact event is called twice.
		if (Cooldown.getCooldown(player.getUniqueId() + "doubleopen") > 0) { //if time left on cooldown is > 0
			return;
		}
		
		Cooldown.addCooldown(player.getUniqueId() + "doubleopen", 1000); //Add cooldown for 1 second
		
		for (final Map.Entry<String, FileConfiguration> menuConfigEntry : Main.getConfigurationManager().getAllMenus().entrySet()) {
			boolean cancel = globalConfig.getBoolean("cancel-click-event", false);
			if (cancel) event.setCancelled(true);
			
			final String configName = menuConfigEntry.getKey();
			final FileConfiguration menuConfig = menuConfigEntry.getValue();
			
			if (!menuConfig.getBoolean("item.enabled")) {
				continue;
			}
			
			ItemStack item = Main.getHotbarItemStackFromMenuConfig(player, menuConfig, configName);
			
			if (!item.isSimilar(player.getItemInHand())) {
				continue;
			}
			
			Main.openSelector(player, configName);
			return;
		}
	}

}
