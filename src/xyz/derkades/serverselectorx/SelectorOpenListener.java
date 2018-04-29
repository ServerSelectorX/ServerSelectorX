package xyz.derkades.serverselectorx;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import xyz.derkades.derkutils.Cooldown;

public class SelectorOpenListener implements Listener {

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event){
		if (event.isCancelled() && Main.getConfigurationManager().getConfig().getBoolean("ignore-cancelled", false)) {
			return;
		}
		
		boolean openOnRightClick = Main.getConfigurationManager().getConfig().getBoolean("right-click-open", true);
		boolean openOnLeftClick = Main.getConfigurationManager().getConfig().getBoolean("left-click-open", false);
		
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
		
		for (Map.Entry<String, FileConfiguration> menuConfigEntry : Main.getConfigurationManager().getAll().entrySet()) {			
			final String configName = menuConfigEntry.getKey();
			final FileConfiguration config = menuConfigEntry.getValue();
			
			if (config.getString("item").equalsIgnoreCase("NONE")){
				continue;
			}
			
			final String string = config.getString("item");
			Material material = Material.getMaterial(string);
			
			if (material == null){
				material = Material.STONE;
			}
			
			//Use deprecated method to still support 1.8.8
			if (player.getInventory().getItemInHand().getType() != material){
				continue;
			}
			
			Main.openSelector(player, config, configName);
			return;
		}
	}

}
