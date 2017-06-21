package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.ListUtils;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.IconMenu;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger.PingException;

public class SelectorMenu extends IconMenu {

	private FileConfiguration config;
	
	public SelectorMenu(String name, int size, Player player, FileConfiguration config) {
		super(Main.getPlugin(), name, size, player);
		this.config = config;
	}

	@Override
	public List<MenuItem> getMenuItems(Player player) {
		final List<MenuItem> list = new ArrayList<>();

		for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = config.getConfigurationSection("menu." + key);

			final int slot = Integer.parseInt(key);
			Material material = Material.getMaterial(section.getString("item"));
			if (material == null) material = Material.STONE;
			final int data = section.getInt("data");
			final ItemStack item = new ItemBuilder(material).setDamage(data).create();
			final String name = Colors.parseColors(section.getString("name"));
			
			List<String> lore = Colors.parseColors(section.getStringList("lore"));
			
			if (section.getBoolean("show-player-count", false)){
				List<String> loreWithPlayerCount = new ArrayList<>();
						
				for (String loreString : lore)
					loreWithPlayerCount.add(loreString.replace("{playercount}", getPlayerCountString(section)));
				
				lore = loreWithPlayerCount;
			}
			
			list.add(new MenuItem(slot, item, name, lore.toArray(new String[]{})));			
		}
		
		return list;
	}

	@Override
	public boolean onOptionClick(OptionClickEvent event) {		
		int slot = event.getPosition();
		Player player = event.getPlayer();
		
		String server = config.getString("menu." + slot + ".server");
		
		if (server.startsWith("url:")){
			//It's a URL
			String url = server.substring(4);
			String message = Colors.parseColors(config.getString("url-message", "&3&lClick here"));
			
			player.spigot().sendMessage(
					new ComponentBuilder(message)
					.event(new ClickEvent(
							ClickEvent.Action.OPEN_URL,
							url))
					.create()
					);
			return true;
		}
		
		if (server.startsWith("cmd:")){
			//It's a command
			String command = server.substring(4);
			Bukkit.dispatchCommand(player, command);
			return true;
		}
		
		if (server.startsWith("sel:")){
			//It's a server selector
			String configName = server.substring(4);
			FileConfiguration config = Main.getSelectorConfigurationFile(configName);
			if (config == null){
				player.sendMessage(ChatColor.RED + "This server selector does not exist.");
				return true;
			} else {
				final int rows = config.getInt("rows");
				final String title = Colors.parseColors(config.getString("title"));
				
				new SelectorMenu(title, rows, player, config).open();
				
				return false;
			}
		}
		
		if (config.getBoolean("menu." + event.getPosition() + ".show-player-count", false) &&
				Main.getPlugin().getConfig().getBoolean("server-offline-message-enabled", true)){
			final String errorMessage = Colors.parseColors(Main.getPlugin().getConfig().getString("ping-error-message-selector", "&cServer is not reachable"));
			if (ListUtils.stringListContainsString(event.getItemStack().getItemMeta().getLore(), errorMessage)){
				//The server is offline
				final String offlineMessage = Colors.parseColors(Main.getPlugin().getConfig().getString("server-offline-message", "server offline"));
				player.sendMessage(offlineMessage);
				return true;
			}
		}
		
		Main.teleportPlayerToServer(player, server);
		
		return true;
	}
	
	private static String getPlayerCountString(ConfigurationSection serverSection){
		String errorMessage = Colors.parseColors( 
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
