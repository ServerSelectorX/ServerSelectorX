package com.robinmc.serverselectorx;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.robinmc.serverselectorx.utils.Config;
import com.robinmc.serverselectorx.utils.IconMenu;
import com.robinmc.serverselectorx.utils.IconMenu.OptionClickEvent;

public class SelectorMenu {
	
	private static IconMenu menu = null; 

	private static void createMenu(String title, int rows){
		menu = new IconMenu(title, rows*9, new IconMenu.OptionClickEventHandler(){

			@Override
			public void onOptionClick(OptionClickEvent event) {
				int slot = event.getPosition();
				Player player = event.getPlayer();
				String server = Config.getConfig().getString("menu." + slot + ".server");
				Main.teleportPlayerToServer(player, server);
			}
			
		});
	}
	
	static void open(Player player){
		int rows = Config.getConfig().getInt("rows");
		String title = Config.getConfig().getString("title");
		createMenu(Main.parseColorCodes(title), rows);
		fillMenu(player);
		menu.open(player);
	}
	
	private static void fillMenu(Player player){
		
		for (String key : Config.getConfig().getConfigurationSection("menu").getKeys(false)){
			ConfigurationSection section = Config.getConfig().getConfigurationSection("menu." + key);
			
			int slot = Integer.parseInt(key);
			Material material = Material.getMaterial(section.getString("item"));
			short data = (short) section.getInt("data");
			ItemStack item = new ItemStack(Material.STONE);
			item.setDurability(data);
			if (material != null) item.setType(material);
			String name = section.getString("name");
			String[] lore = section.getStringList("lore").toArray(new String[]{});
			
			menu.setOption(slot, item, name, lore);
		}
	}

}
