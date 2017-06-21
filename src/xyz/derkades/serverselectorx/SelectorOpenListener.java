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

import xyz.derkades.derkutils.bukkit.Colors;

public class SelectorOpenListener implements Listener {
	
	private static final List<UUID> COOLDOWN = new ArrayList<>();
	
	@EventHandler(priority = EventPriority.HIGH)
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
				
				
			final boolean permissionsEnabled = config.getBoolean("permissions-enabled");
			final boolean hasPermission = player.hasPermission("ssx.use." + config.getName().replace(".yml", ""));
			if (!permissionsEnabled || hasPermission){
				
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
				
				new BukkitRunnable(){
					public void run(){
						final int rows = config.getInt("rows");
						final String title = Colors.parseColors(config.getString("title"));
						
						new SelectorMenu(title, rows * 9, player, config).open();
					}
				}.runTaskAsynchronously(Main.getPlugin());
				
		} else if (config.getBoolean("no-permission-message-enabled", false))
			player.sendMessage(config.getString("no-permission-message"));
			return;
		}
	}

}
