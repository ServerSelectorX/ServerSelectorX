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
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.IconMenu;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.serverselectorx.utils.Cache;
import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger.PingException;

public class SelectorMenu extends IconMenu {

	private FileConfiguration config;
	private Player player;
	
	public SelectorMenu(String name, int size, Player player, FileConfiguration config) {
		super(Main.getPlugin(), name, size, player);
		this.config = config;
		this.player = player;
	}

	@Override
	public void open() {
		new BukkitRunnable(){
			public void run(){
				for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
					final ConfigurationSection section = config.getConfigurationSection("menu." + key);

					final int slot = Integer.parseInt(key);
					Material material = Material.getMaterial(section.getString("item"));
					if (material == null) material = Material.STONE;
					final int data = section.getInt("data");
					final String name = section.getString("name");
					
					final ItemBuilder builder = new ItemBuilder(material).data(data);
					
					//Apply custom glowing enchantment
					if (section.getBoolean("enchanted", false)){
						builder.unsafeEnchant(new GlowEnchantment(), 1);
					}
					
					//If ping server is turned off just add item and continue to next server
					if (!section.getBoolean("ping-server")){
						//Run synchronously, because PlaceholderAPI uses the Bukkit API
						new BukkitRunnable(){
							public void run(){
								List<String> lore = Main.PLACEHOLDER_API.parsePlaceholders(player, section.getStringList("lore"));
								items.put(slot, builder.name(Main.PLACEHOLDER_API.parsePlaceholders(player, name)).lore(lore).create());
							}
						}.runTask(Main.getPlugin());
						continue;
					}
					
					//Server pinging is turned on, continue to run stuff below async
					
					if (Cache.getCachedObject(name + "serverOnline") == null || 
							Cache.getCachedObject(name + "motd") == null || 
							Cache.getCachedObject(name + "onlinePlayers") == null || 
							Cache.getCachedObject(name + "maxPlayers") == null ||
							Cache.getCachedObject(name + "consoleErrorMessage") == null){
						//Not cached, ping server
						boolean serverOnline;
						String consoleErrorMessage = "";

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
						
						//Cache objects with a timeout of 3,5 seconds
						Cache.addCachedObject(name + "serverOnline", serverOnline, 3);
						Cache.addCachedObject(name + "consoleErrorMessage", consoleErrorMessage, 3);
						Cache.addCachedObject(name + "motd", motd, 3);
						Cache.addCachedObject(name + "onlinePlayers", onlinePlayers, 3);
						Cache.addCachedObject(name + "maxPlayers", maxPlayers, 3);
					}
					
					final boolean serverOnline = (boolean) Cache.getCachedObject(name + "serverOnline");
					final String consoleErrorMessage = (String) Cache.getCachedObject(name + "consoleErrorMessage");

					final String motd = (String) Cache.getCachedObject(name + "motd");
					final int onlinePlayers = (int) Cache.getCachedObject(name + "onlinePlayers");
					final int maxPlayers = (int) Cache.getCachedObject(name + "maxPlayers");
					
					new BukkitRunnable(){
						
						public void run(){						
							List<String> lore = new ArrayList<>();;

							if (serverOnline) {
								if (section.getBoolean("change-item-count", true)) {
									int amount = onlinePlayers;
									if (amount > 64 || amount < 1)
										amount = 1;
									builder.amount(amount);
								}

								for (String loreString : section.getStringList("lore")) {
									lore.add(Main.PLACEHOLDER_API.parsePlaceholders(player, loreString)
											.replace("{online}", String.valueOf(onlinePlayers))
											.replace("{max}", String.valueOf(maxPlayers)).replace("{motd}", motd));
								}
							} else {
								if (Main.getPlugin().getConfig().getBoolean("ping-error-message-console", true)) {
									Main.getPlugin().getLogger().log(Level.SEVERE, String.format(
											"An error occured while trying to ping %s. (%s)", name, consoleErrorMessage));
								}

								Material offlineType = Material.getMaterial(section.getString("offline-item", "error"));
								if (offlineType != null) {
									builder.type(offlineType).data(section.getInt("offline-data", 0));
								}

								lore = Main.PLACEHOLDER_API.parsePlaceholders(player, section.getStringList("offline-lore"));
							}

							items.put(slot, builder.lore(lore).name(Main.PLACEHOLDER_API.parsePlaceholders(player, name)).create());
						}
					}.runTask(Main.getPlugin());
				}
				
				//After for loop has completed, open menu synchronously
				new BukkitRunnable(){
					public void run(){
						callOriginalOpenMethod();
					}
				}.runTaskLater(Main.getPlugin(), 1); //Wait a tick for safety. It's unnoticeable anyways
			}
		}.runTaskAsynchronously(Main.getPlugin());
		
	}
	
	private void callOriginalOpenMethod(){
		super.open();
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
			
			//Send command 1 tick later to let the GUI close first (for commands that open a GUI)
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
				Bukkit.dispatchCommand(player, command);
			}, 1);
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

}
