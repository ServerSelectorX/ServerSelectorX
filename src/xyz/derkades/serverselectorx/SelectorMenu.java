package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.IconMenu;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger.Server;

public class SelectorMenu extends IconMenu {

	private FileConfiguration config;
	private Player player;
	
	public SelectorMenu(Player player, FileConfiguration config) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", "no title")), config.getInt("rows", 6) * 9, player);
		
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
						Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
							List<String> lore = Main.PLACEHOLDER_API.parsePlaceholders(player, section.getStringList("lore"));
							items.put(slot, builder.name(Main.PLACEHOLDER_API.parsePlaceholders(player, name)).lore(lore).create());
						});
						continue;
					}
					
					//Server pinging is turned on, continue to run stuff below async
					
					String ip = section.getString("ip");
					int port = section.getInt("port");
					
					Server server;
					
					if (Main.getPlugin().getConfig().getBoolean("external-query", true)){
						server = new ServerPinger.ExternalServer(ip, port);
					} else {
						int timeout = section.getInt("ping-timeout", 100);
						server = new ServerPinger.InternalServer(ip, port, timeout);
					}
					
					boolean online = server.isOnline();
					String motd = server.getMotd();
					int onlinePlayers = server.getOnlinePlayers();
					int maxPlayers = server.getMaximumPlayers();
					int ping = server.getResponseTimeMillis();
					
					//No need to run async anymore
					
					Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {					
						List<String> lore = new ArrayList<>();
						
						if (online) {
							if (section.getBoolean("change-item-count", true)) {
								String mode = Main.getPlugin().getConfig().getString("item-count-mode", "absolute");
								int amount;
								
								if (mode.equals("absolute")) {
									amount = onlinePlayers;
								} else if (mode.equals("relative")) {
									amount = (onlinePlayers / maxPlayers) * 100;
								} else {
									amount = 1;
									Main.getPlugin().getLogger().warning("item-count-mode setting is invalid");
								}
									
								if (amount > 64 || amount < 1)
									amount = 1;
									
								builder.amount(amount);
							}

							for (String loreString : section.getStringList("lore")) {
								lore.add(Main.PLACEHOLDER_API.parsePlaceholders(player, loreString)
										.replace("{online}", String.valueOf(onlinePlayers))
										.replace("{max}", String.valueOf(maxPlayers))
										.replace("{motd}", motd)
										.replace("{ping}", String.valueOf(ping)));
							}
						} else {
							Material offlineType = Material.getMaterial(section.getString("offline-item", "error"));
							if (offlineType != null) {
								builder.type(offlineType).data(section.getInt("offline-data", 0));
							}

							lore = Main.PLACEHOLDER_API.parsePlaceholders(player, section.getStringList("offline-lore"));
						}

						items.put(slot, builder.lore(lore).name(Main.PLACEHOLDER_API.parsePlaceholders(player, name)).create());
					});
				}
				
				//After for loop has completed, open menu synchronously
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
					Cooldown.addCooldown(config.getName() + player.getName(), 0); //Remove cooldown if menu opened successfully
					callOriginalOpenMethod();
				}, 1); //Wait a tick just in case. It's unnoticeable anyways
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
		} else if (action.startsWith("bungeecmd:")) { //BungeeCord command
			String command = action.split(":")[0];
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("sync player %s %s", player.getName(), command));
			return true;
		} else if (action.startsWith("sel:")){ //Open selector
			//It's a server selector
			String configName = action.substring(4);
			FileConfiguration config = Main.getSelectorConfigurationFile(configName);
			if (config == null){
				player.sendMessage(ChatColor.RED + "This server selector does not exist.");
				return true;
			} else {				
				new SelectorMenu(player, config).open();
				
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
			return true; //Return true = close
		} else {
			return false; //Return false = stay open
		}
	
	}

}
