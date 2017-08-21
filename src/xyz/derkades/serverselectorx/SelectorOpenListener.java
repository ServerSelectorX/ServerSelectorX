package xyz.derkades.serverselectorx;

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
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event){
		if (!(event.getAction() == Action.RIGHT_CLICK_AIR ||
				event.getAction() == Action.RIGHT_CLICK_BLOCK)){
			return;
		}
		
		Player player = event.getPlayer();
		
		//Small cooldown because on 1.9+ interact event is called twice.
		if (Cooldown.getCooldown(player.getUniqueId() + "doubleopen") > 0) {
			return;
		} else {
			Cooldown.addCooldown(player.getUniqueId() + "doubleopen", 100); //Add cooldown for 0.1 seconds
		}
		
		for (FileConfiguration config : Main.getServerSelectorConfigurationFiles()){	
			if (config.getString("item").equalsIgnoreCase("NONE")){
				continue;
			}
			
			final String string = config.getString("item");
			final Material material = Material.getMaterial(string);
			
			if (material == null){
				player.sendMessage(Message.INVALID_ITEM_NAME.toString());
				return;
			}
			
			if (player.getInventory().getItemInHand().getType() != material){
				continue;
			}
			
			Main.openSelector(player, config);
			return;
		}
	}

}
