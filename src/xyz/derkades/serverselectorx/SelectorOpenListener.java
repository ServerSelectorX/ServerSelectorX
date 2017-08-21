package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SelectorOpenListener implements Listener {
	
	private static final List<UUID> COOLDOWN = new ArrayList<>();
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event){
		if (!(event.getAction() == Action.RIGHT_CLICK_AIR ||
				event.getAction() == Action.RIGHT_CLICK_BLOCK)){
			return;
		}
		
		Player player = event.getPlayer();
		
		if (COOLDOWN.contains(player.getUniqueId())){
			return;
		}
		
		COOLDOWN.add(player.getUniqueId());
		
		new BukkitRunnable(){
			public void run(){
				COOLDOWN.remove(player.getUniqueId());
			}
		}.runTaskLater(Main.getPlugin(), 10);
		
		
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
