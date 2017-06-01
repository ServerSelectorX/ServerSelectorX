package xyz.derkades.serverselectorx;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.serverselectorx.utils.Config;
import xyz.derkades.serverselectorx.utils.IconMenu;
import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.IconMenu.OptionClickEvent;
import xyz.derkades.serverselectorx.utils.ServerPinger.PingException;

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
				String errorMessage = ChatColor.translateAlternateColorCodes('&', 
						Config.getConfig().getString("ping-error-message-selector", "&cServer is not reachable"));
				try {
					String ip = section.getString("ip");
					int port = section.getInt("port");

					int timeout = section.getInt("ping-timeout", 100);
					
					String[] result = ServerPinger.pingServer(ip, port, timeout);
					
					if (result != null){
						int onlinePlayers = Integer.parseInt(result[1]);
						int maxPlayers = Integer.parseInt(result[2]);
						String message = section.getString("player-count")
								.replace("{x}", onlinePlayers + "")
								.replace("{y}", maxPlayers + "");
						lore.add(message);
					} else {
						lore.add(errorMessage);
					}
				} catch (PingException e) {
					boolean sendErrorMessage = Config.getConfig().getBoolean("ping-error-message-console", true);
					if (sendErrorMessage) Main.getPlugin().getLogger().log(Level.SEVERE, "An error occured while trying to ping " + name + ".");
					
					lore.add(errorMessage);
				}
			}

			menu.setOption(slot, item, name, lore.toArray(new String[]{}));
		}
		
		/*boolean autoRefresh = Config.getConfig().getBoolean("auto-refresh");
		if (autoRefresh){
			new BukkitRunnable(){
				public void run(){
					//Don't re-open the menu if the player has closed the menu.
					if (player.getOpenInventory().getType() != InventoryType.CHEST){
						this.cancel();
					} else open(player);
				}
			}.runTaskTimer(Main.getPlugin(), 50, 50);
		}*/
		
	}

}
