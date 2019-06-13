package xyz.derkades.serverselectorx;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class OnJoinListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		for (Map.Entry<String, FileConfiguration> menuConfigEntry : 
			Main.getConfigurationManager().getAllMenus().entrySet()) {			
			
			final FileConfiguration menuConfig = menuConfigEntry.getValue();
			final String configName = menuConfigEntry.getKey();
			
			if (!menuConfig.getBoolean("item.enabled"))
				continue; // Item is disabled
			
			if (!menuConfig.getBoolean("item.on-join.enabled"))
				continue; // Item on join is disbled

			
			if (menuConfig.getBoolean("permission.item")) {
				if (!player.hasPermission("ssx.item." + configName)) {
					final String permission = "ssx.item." + configName;
					if (!player.hasPermission(permission)) {
						if (menuConfig.getBoolean("permission.debug")) {
							Main.getPlugin().getLogger().info(String.format("%s did not receive an item because they don't have the permission %s", 
									player.getName(), permission));
						}
						continue;
					}
				}
			}
			
			ItemStack item = Main.getHotbarItemStackFromMenuConfig(player, menuConfig, configName);
			
			int slot = menuConfig.getInt("item.on-join.inv-slot", 0);
			PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(item, 1)) {
					inv.addItem(item);
				}
			} else {
				inv.setItem(slot, item);
			}
		}		
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void applyEffects(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		FileConfiguration global = Main.getConfigurationManager().getGlobalConfig();
		
		if (global.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			event.getPlayer().getInventory().clear();
		}
		
		if (global.getBoolean("speed-on-join", false)) {
			int amplifier = global.getInt("speed-amplifier", 3);
			
			if (global.getBoolean("show-particles")) 
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true));
			else
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true, false));
		}
		
		if (global.getBoolean("hide-self-on-join", false)) {
			if (global.getBoolean("show-particles")) 
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true));
			else
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
		}
		
		if (global.getBoolean("hide-others-on-join", false)) {
			InvisibilityToggle.hideOthers(player);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void openMenu(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			
			for (Map.Entry<String, FileConfiguration> menuConfigEntry : 
				Main.getConfigurationManager().getAllMenus().entrySet()) {
				
				if (menuConfigEntry.getValue().getBoolean("open-on-join", false)) {
					Main.openSelector(player, menuConfigEntry.getKey());
				}
			}
		}, 5);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void sendAnnoyingMessage(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("ssx.reload") && Main.BETA) {
			player.sendMessage("You are using a beta version of ServerSelectorX. Please update to a stable version as soon as the functionality or bugfix you require is available in a stable release version. " + ChatColor.GRAY + "(only players with the ssx.admin permission will see this message)");
		}
	}

}
