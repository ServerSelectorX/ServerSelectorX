package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.ListUtils;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;

public class SelectorMenu extends IconMenu {

	/*
	 * This code is very messy. I am sorry :(
	 * I keep adding new stuff quickly without thinking about code design.
	 * I promise, I'll fix it soon(tm)!
	 */
	
	private FileConfiguration config;
	private Player player;
	private int slots;
	
	public SelectorMenu(Player player, FileConfiguration config) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", "no title")), config.getInt("rows", 6) * 9, player);
		
		this.config = config;
		this.player = player;
		this.slots = config.getInt("rows", 6) * 9;
	}

	@Override
	public void open() {
		new BukkitRunnable(){
			public void run(){
				for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
					final ConfigurationSection section = config.getConfigurationSection("menu." + key);
					
					String materialString = "STONE";
					int data = 0;
					String name = "";
					List<String> lore = new ArrayList<>();
					int amount = 1;
					
					String action = section.getString("action");
					
					if (action.startsWith("srv:")) {
						String serverName = action.substring(4);
						
						if (!section.getBoolean("ping-server")){
							//Server pinging is turned off, get item info from 'online' section
							materialString = section.getString("online.item");
							data = section.getInt("online.data", 0);
							name = section.getString("online.name", "");
							lore = section.getStringList("online.lore");
							amount = section.getInt("item-count", 1);
						} else {
							Map<String, Object> placeholders = null;
							boolean isOnline;
							if (Main.SERVER_PLACEHOLDERS.containsKey(serverName)) {
								placeholders = Main.SERVER_PLACEHOLDERS.get(serverName);
								isOnline = (boolean) placeholders.get("isOnline");
							} else {
								isOnline = false;
							}
							
							if (isOnline) {
								int online = (int) placeholders.get("online");
								int max = (int) placeholders.get("max");
								int ping = (int) placeholders.get("ping");
								String motd = (String) placeholders.get("motd");
									
								//Replace placeholders in lore
								lore = ListUtils.replaceInStringList(lore, 
										new Object[] {"{online}", "{max}", "{motd}", "{ping}", "{player}"},
										new Object[] {online, max, motd, ping, player.getName()});
									
								amount = section.getInt("item-count", 1);
								
								if (section.getBoolean("change-item-count", true)) {
									amount = online;
								}
							} else {
								//Server is offline
								ConfigurationSection offlineSection = section.getConfigurationSection("offline");
										
								materialString = offlineSection.getString("item");
								data = offlineSection.getInt("data", 0);
								name = offlineSection.getString("name");
								lore = offlineSection.getStringList("lore");
							}
						}
								
						if (amount > 64 || amount < 1)
							amount = 1;
					}
					
					final ItemBuilder builder;
					
					if (materialString.startsWith("head:")) {
						String owner = materialString.split(":")[1];
						if (owner.equals("auto")) {
							builder = new ItemBuilder(player.getName());
						} else {
							builder = new ItemBuilder(owner);
						}
					} else {
						Material material = Material.valueOf(materialString);
						if (material == null) material = Material.STONE;
						
						builder = new ItemBuilder(material);
						builder.data(data);
					}					

					builder.name(Main.PLACEHOLDER_API.parsePlaceholders(player, name));
					builder.lore(Main.PLACEHOLDER_API.parsePlaceholders(player, lore));
						
					ItemStack item = builder.create();
						
					addToMenu(Integer.valueOf(key), item);
				}
				
				//After everything has completed, open menu synchronously
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
	
	private void addToMenu(int slot, ItemStack item) {
		if (slot < 0) {
			for (int i = 0; i < slots; i++) {
				if (!items.containsKey(i)) {
					items.put(i, item);
				}
			}
		} else {
			items.put(slot, item);
		}
	}

	@Override
	public boolean onOptionClick(OptionClickEvent event) {		
		int slot = event.getPosition();
		Player player = event.getPlayer();
		
		String action = config.getString("menu." + slot + ".action");
		
		if (action == null) {
			//If the action is null (so 'slot' is not found in the config) it is probably a wildcard
			action = config.getString("menu.-1.action");
			
			if (action == null) { //If it is still null it must be missing
				action = "msg:Action missing";
			}
		}
		
		if (action.startsWith("url:")){ //Send url message
			String url = action.substring(4);
			String message = Colors.parseColors(config.getString("url-message", "&3&lClick here"));
			
			player.spigot().sendMessage(
					new ComponentBuilder(message)
					.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
					.create()
					);
			return true;
		} else if (action.startsWith("cmd:")){ //Execute command
			String command = action.substring(4);
			
			//Send command 2 ticks later to let the GUI close first (for commands that open a GUI)
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
				Bukkit.dispatchCommand(player, Main.PLACEHOLDER_API.parsePlaceholders(player, command));
			}, 2);
			return true;
		} else if (action.startsWith("bungeecmd:")) { //BungeeCord command
			String command = action.substring(10);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("sync player bungee %s %s", player.getName(), Main.PLACEHOLDER_API.parsePlaceholders(player, command)));
			return true;
		} else if (action.startsWith("sel:")){ //Open selector
			String configName = action.substring(4);
			FileConfiguration config = Main.getConfigurationManager().getByName(configName);
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
		} else if (action.startsWith("msg:")){ //Send message
			String message = action.substring(4);
			player.sendMessage(Main.PLACEHOLDER_API.parsePlaceholders(player, message));
			return true;
		} else if (action.equals("close")){ //Close selector
			return true; //Return true = close
		} else {
			return false; //Return false = stay open
		}
	
	}

}
