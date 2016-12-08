package com.robinmc.serverselectorx;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.robinmc.serverselectorx.utils.Config;

public class SelectorOpenListener implements Listener {
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInteract(PlayerInteractEvent event){
		if (!(event.getAction() == Action.RIGHT_CLICK_AIR ||
				event.getAction() == Action.RIGHT_CLICK_BLOCK)){
			return;
		}
		
		Player player = event.getPlayer();
		
		String string = Config.getConfig().getString("item");
		Material material = Material.getMaterial(string);
		
		if (material == null){
			player.sendMessage(Message.INVALID_ITEM_NAME.toString());
			return;
		}
		
		if (player.getInventory().getItemInHand().getType() == material){
			boolean permissionsEnabled = Config.getConfig().getBoolean("permissions-enabled");
			if (permissionsEnabled){
				boolean hasPermission = player.hasPermission("ssx.use");
				if (!hasPermission){
					if (Config.getConfig().getBoolean("no-permission-message-enabled"))
						player.sendMessage(Config.getConfig().getString("no-permission-message"));
					return;
				}
			}
			SelectorMenu.open(player);
		}
	}

}
