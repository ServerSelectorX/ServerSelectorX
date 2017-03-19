package com.robinmc.serverselectorx;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.robinmc.serverselectorx.utils.Config;
import com.robinmc.serverselectorx.utils.IconMenu;
import com.robinmc.serverselectorx.utils.IconMenu.OptionClickEvent;
import com.robinmc.serverselectorx.utils.ServerPinger;
import com.robinmc.serverselectorx.utils.ServerPinger.PingException;

public class SelectorMenu {

	private static IconMenu menu = null;

	private static void createMenu(String title, int rows) {
		menu = new IconMenu(title, rows * 9, new IconMenu.OptionClickEventHandler() {

			@Override
			public void onOptionClick(OptionClickEvent event) {
				int slot = event.getPosition();
				Player player = event.getPlayer();
				String server = Config.getConfig().getString("menu." + slot + ".server");
				Main.teleportPlayerToServer(player, server);
			}

		});
	}

	static void open(Player player) {
		int rows = Config.getConfig().getInt("rows");
		String title = Config.getConfig().getString("title");
		createMenu(Main.parseColorCodes(title), rows);
		fillMenu(player);
		menu.open(player);
	}

	private static void fillMenu(Player player) {

		for (String key : Config.getConfig().getConfigurationSection("menu").getKeys(false)) {
			ConfigurationSection section = Config.getConfig().getConfigurationSection("menu." + key);

			int slot = Integer.parseInt(key);
			Material material = Material.getMaterial(section.getString("item"));
			short data = (short) section.getInt("data");
			ItemStack item = new ItemStack(Material.STONE);
			item.setDurability(data);
			if (material != null)
				item.setType(material);
			String name = section.getString("name");
			List<String> lore = section.getStringList("lore");

			if (section.getBoolean("show-player-count")) {
				
				String ip = section.getString("ip");
				int port = section.getInt("port");
				int onlinePlayers = 0;
				int maxPlayers = 0;
				
				try {
					String[] result = ServerPinger.pingServer(ip, port);
					onlinePlayers = Integer.parseInt(result[1]);
					maxPlayers = Integer.parseInt(result[2]);
				} catch (PingException e) {
					Main.getPlugin().getLogger().log(Level.SEVERE, "An error occured while trying to ping " + name + ". IP: " + ip + ":" + port);
				}
				
				String message = section.getString("player-count")
						.replace("{x}", onlinePlayers + "")
						.replace("{y}", maxPlayers + "");
				lore.add(message);
			}

			menu.setOption(slot, item, name, lore.toArray(new String[]{}));
		}
		
		boolean autoRefresh = Config.getConfig().getBoolean("auto-refresh");
		if (autoRefresh){
			new BukkitRunnable(){
				public void run(){
					//Don't re-open the menu if the player has closed the menu.
					if (player.getOpenInventory().getType() != InventoryType.CHEST){
						this.cancel();
					} else menu.open(player);
				}
			}.runTaskTimer(Main.getPlugin(), 50, 50);
		}
		
	}

}
