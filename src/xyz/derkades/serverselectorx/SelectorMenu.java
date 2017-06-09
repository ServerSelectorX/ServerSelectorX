package xyz.derkades.serverselectorx;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.serverselectorx.utils.IconMenu;
import xyz.derkades.serverselectorx.utils.IconMenu.OptionClickEvent;
import xyz.derkades.serverselectorx.utils.ItemBuilder;
import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger.PingException;

public class SelectorMenu {

	private static IconMenu getMenu(String title, int rows) {
		return new IconMenu(title, rows * 9, new IconMenu.OptionClickEventHandler() {

			@Override
			public void onOptionClick(OptionClickEvent event) {
				FileConfiguration config = Main.getPlugin().getConfig();
				
				int slot = event.getPosition();
				Player player = event.getPlayer();
				
				String server = config.getString("menu." + slot + ".server");
				
				if (server.startsWith("url:")){
					//It's a URL
					String url = server.substring(4);
					String message = Main.parseColorCodes(config.getString("url-message", "&3&lClick here"));
					
					player.spigot().sendMessage(
							new ComponentBuilder(message)
							.event(new ClickEvent(
									ClickEvent.Action.OPEN_URL,
									url))
							.create()
							);
				}
				
				Main.teleportPlayerToServer(player, server);
			}

		});
	}

	static void open(final Player player) {
		FileConfiguration config = Main.getPlugin().getConfig();
		
		final int rows = config.getInt("rows");
		final String title = config.getString("title");
		final IconMenu menu = getMenu(Main.parseColorCodes(title), rows);

		for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = config.getConfigurationSection("menu." + key);

			final int slot = Integer.parseInt(key);
			Material material = Material.getMaterial(section.getString("item"));
			if (material == null) material = Material.STONE;
			final int data = section.getInt("data");
			final ItemStack item = new ItemBuilder(material).setDamage(data).create();
			String name = section.getString("name");
			final List<String> lore = section.getStringList("lore");

			final boolean showPlayerCount = section.getBoolean("show-player-count", false);
			
			new BukkitRunnable(){
				public void run(){
					if (showPlayerCount){
						lore.add(getPlayerCountString(section));
					}
					
					//Now go sync again because bukkit API should only be used in the main server thread
					new BukkitRunnable(){
						public void run(){
							menu.setOption(slot, item, name, lore.toArray(new String[]{}));
							menu.open(player);
						}
					}.runTask(Main.getPlugin());
				}
			}.runTaskAsynchronously(Main.getPlugin());
			
		}
		
	}

	private static String getPlayerCountString(ConfigurationSection serverSection){
		String errorMessage = ChatColor.translateAlternateColorCodes('&', 
				Main.getPlugin().getConfig().getString("ping-error-message-selector", "&cServer is not reachable"));
		
		try {
			String ip = serverSection.getString("ip");
			int port = serverSection.getInt("port");

			int timeout = serverSection.getInt("ping-timeout", 100);
			
			String[] result = ServerPinger.pingServer(ip, port, timeout);
			
			if (result != null){
				int onlinePlayers = Integer.parseInt(result[1]);
				int maxPlayers = Integer.parseInt(result[2]);
				String message = serverSection.getString("player-count")
						.replace("{x}", onlinePlayers + "")
						.replace("{y}", maxPlayers + "");
				return message;
			} else {
				return errorMessage;
			}
		} catch (PingException e) {
			boolean sendErrorMessage = Main.getPlugin().getConfig().getBoolean("ping-error-message-console", true);
			if (sendErrorMessage) Main.getPlugin().getLogger().log(Level.SEVERE, "An error occured while trying to ping " + serverSection.getString("name") + ". (" + e.getMessage() + ")");
			
			return errorMessage;
		}
	}

}
