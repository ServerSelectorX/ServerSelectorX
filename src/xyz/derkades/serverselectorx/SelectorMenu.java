package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
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
			final String name = placeholders(player, Colors.parseColors(section.getString("name")));
			
			//Apply custom glowing enchantment
			if (section.getBoolean("enchanted", false)){
				ItemMeta meta = item.getItemMeta();
				meta.addEnchant(new GlowEnchantment(), 1, true);
				item.setItemMeta(meta);
			}
			
			//If ping server is turned off just add item and continue to next server
			if (!section.getBoolean("ping-server")){
				//List<String> lore = Colors.parseColors(section.getStringList("lore"));
				List<String> lore = new ArrayList<>();
				for (String loreString : section.getStringList("lore")){
					lore.add(Colors.parseColors(placeholders(player, loreString)));
				}
				list.add(new MenuItem(slot, item, name, lore.toArray(new String[]{})));	
				continue;
			}
			
			boolean serverOnline;
			String consoleErrorMessage = null;

			String motd = "";
			int onlinePlayers = 0;
			int maxPlayers = 0;

			try {
				String ip = section.getString("ip");
				int port = section.getInt("port");
				int timeout = section.getInt("ping-timeout", 100);

				String[] result = ServerPinger.pingServer(ip, port, timeout);

				if (result == null) {
					serverOnline = false;
					consoleErrorMessage = "Server not reachable within set timeout";
				} else {
					motd = result[0];
					onlinePlayers = Integer.parseInt(result[1]);
					maxPlayers = Integer.parseInt(result[2]);
					serverOnline = true;
				}
			} catch (PingException e) {
				serverOnline = false;
				consoleErrorMessage = e.getMessage();
			}

			List<String> lore = new ArrayList<>();;

			if (serverOnline) {
				if (section.getBoolean("change-item-count", true)) {
					int amount = onlinePlayers;
					if (amount > 64)
						amount = 1;
					item.setAmount(amount);
				}
				
				for (String loreString : section.getStringList("lore")){
					lore.add(Colors.parseColors(placeholders(player, loreString))
							.replace("{online}", String.valueOf(onlinePlayers))
							.replace("{max}", String.valueOf(maxPlayers))
							.replace("{motd}", motd));
				}
			} else {
				if (Main.getPlugin().getConfig().getBoolean("ping-error-message-console", true)) {
					Main.getPlugin().getLogger().log(Level.SEVERE, String.format("An error occured while trying to ping %s. (%s)", name, consoleErrorMessage));
				}

				Material offlineType = Material.getMaterial(section.getString("offline-item", "error"));
				if (offlineType != null) {
					item.setType(offlineType);
					item.setDurability((short) section.getInt("offline-data", 0));
				}

				for (String loreString : section.getStringList("lore")){
					lore.add(Colors.parseColors(placeholders(player, loreString)));
				}
			}
			
			list.add(new MenuItem(slot, item, name, lore.toArray(new String[]{})));
		}
		
		return list;
	}

	@Override
	public boolean onOptionClick(OptionClickEvent event) {		
		int slot = event.getPosition();
		Player player = event.getPlayer();
		
		String action = config.getString("menu." + slot + ".action");
		
		if (action.startsWith("url:")){ //Send url message
			//It's a URL
			String url = action.substring(4);
			String message = Colors.parseColors(config.getString("url-message", "&3&lClick here"));
			
			player.spigot().sendMessage(
					new ComponentBuilder(message)
					.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
					.create()
					);
			return true;
		} else if (action.startsWith("cmd:")){ //Execute command
			//It's a command
			String command = action.substring(4);
			Bukkit.dispatchCommand(player, command);
			return true;
		} else if (action.startsWith("sel:")){ //Open selector
			//It's a server selector
			String configName = action.substring(4);
			FileConfiguration config = Main.getSelectorConfigurationFile(configName);
			if (config == null){
				player.sendMessage(ChatColor.RED + "This server selector does not exist.");
				return true;
			} else {
				final int rows = config.getInt("rows");
				final String title = Colors.parseColors(config.getString("title"));
				
				new SelectorMenu(title, rows * 9, player, config).open();
				
				return false;
			}
		} else if (action.startsWith("world:")){ //Teleport to world
			String worldName = action.substring(6);
			World world = Bukkit.getWorld(worldName);
			if (world == null){
				player.sendMessage(ChatColor.RED + "A world with the name " + worldName + " does not exist.");
				return true;
			} else {
				player.teleport(world.getSpawnLocation());
				return true;
			}
		} else if (action.startsWith("srv:")){ //Teleport to server
			String serverName = action.substring(4);
			Main.teleportPlayerToServer(player, serverName);
			return true;
		} else if (action.startsWith("msg")){ //Send message
			String message = action.substring(4);
			player.sendMessage(Colors.parseColors(message));
			return true;
		} else if (action.equals("close")){ //Close selector
			return true;
		} else {
			return false;
		}
	
	}
	
	private static String placeholders(Player player, String text){
		return Main.PLACEHOLDER_API.parsePlaceholders(player, text);
	}

}
