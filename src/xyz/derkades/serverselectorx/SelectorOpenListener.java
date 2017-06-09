package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.Sound;
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
	
	@EventHandler(priority = EventPriority.MONITOR)
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
		
		FileConfiguration config = Main.getPlugin().getConfig();
		
		String string = config.getString("item");
		Material material = Material.getMaterial(string);
		
		if (material == null){
			player.sendMessage(Message.INVALID_ITEM_NAME.toString());
			return;
		}
		
		if (player.getInventory().getItemInHand().getType() == material){
			boolean permissionsEnabled = config.getBoolean("permissions-enabled");
			if (permissionsEnabled){
				boolean hasPermission = player.hasPermission("ssx.use");
				if (!hasPermission){
					if (config.getBoolean("no-permission-message-enabled"))
						player.sendMessage(config.getString("no-permission-message"));
					return;
				}
			}
			
			//Play sound
			String soundString = config.getString("selector-open-sound");
			if (soundString != null && !soundString.equals("NONE")){
				try {
					Sound sound = Sound.valueOf(soundString);
					player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
				} catch (IllegalArgumentException e){
					Main.getPlugin().getLogger().log(Level.WARNING, "A sound with the name " + soundString + " could not be found. Make sure that it is the right name for your server version.");
				}
			}
			
			SelectorMenu.open(player);
		}
	}

}
