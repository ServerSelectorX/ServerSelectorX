package xyz.derkades.serverselectorx;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.derkades.derkutils.bukkit.ItemBuilder;

public class OnJoinListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		FileConfiguration config = Main.getConfigurationManager().getConfig();
		
		if (config.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			event.getPlayer().getInventory().clear();
		}
		
		if (config.getBoolean("speed-on-join", false)) {
			int amplifier = Main.getConfigurationManager().getConfig().getInt("speed-amplifier", 3);
			
			if (Main.getConfigurationManager().getConfig().getBoolean("show-particles")) 
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true));
			else
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true, false));
		}
		
		if (config.getBoolean("hide-self-on-join", false)) {
			if (Main.getConfigurationManager().getConfig().getBoolean("show-particles")) 
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true));
			else
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
		}
		
		if (config.getBoolean("hide-others-on-join", false)) {
			InvisibilityToggle.hideOthers(player);
		}
		
		for (Map.Entry<String, FileConfiguration> menuConfigEntry : Main.getConfigurationManager().getAll().entrySet()) {			
			//final String configName = menuConfigEntry.getKey();
			final FileConfiguration menuConfig = menuConfigEntry.getValue();
			boolean putItemInInventory = menuConfig.getBoolean("on-join");
			if (!putItemInInventory)
				continue;
			
			
			
			if (Main.getPlugin().getConfig().getBoolean("permissions-enabled")) {
				if (!player.hasPermission("ssx.join." + menuConfig.getName().replace(".yml", ""))) {
					continue;
				}
			}
			
			if (menuConfig.getString("item").equals("NONE")) { 
				continue;
			}
			
			Material material = Material.getMaterial(menuConfig.getString("item"));
			
			if (material == null) {
				material = Material.STONE;
			}
			
			String name = Main.PLACEHOLDER_API.parsePlaceholders(player,
					menuConfig.getString("item-name", "error")
					.replace("{player}", player.getName()));
			
			ItemBuilder builder = new ItemBuilder(material)
					.data(menuConfig.getInt("data", 0))
					.coloredName(name);
			
			List<String> lore = Main.PLACEHOLDER_API.parsePlaceholders(player, menuConfig.getStringList("item-lore"));
			
			//Don't add lore if it's null or if the first line is equal to 'none'
			if (!(lore == null || lore.isEmpty() || lore.get(0).equalsIgnoreCase("none"))) {
				builder.coloredLore(lore);
			}
			
			int slot = menuConfig.getInt("inv-slot", 0);
			PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(builder.create(), 1)) {
					inv.addItem(builder.create());
				}
			} else {
				inv.setItem(slot, builder.create());
			}
		}
		
		String openOnJoin = Main.getConfigurationManager().getConfig().getString("open-on-join", "none");
		
		if (!openOnJoin.equals("none")) {
			Main.openSelector(player, Main.getConfigurationManager().getByName(openOnJoin), openOnJoin);
		}
	}

}
