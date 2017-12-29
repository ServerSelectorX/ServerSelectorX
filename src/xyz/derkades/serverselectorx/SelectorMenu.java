package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.IconMenu;
import xyz.derkades.derkutils.bukkit.ItemBuilder;

public class SelectorMenu extends IconMenu {
	
	private FileConfiguration config;
	private Player player;
	private int slots;
	
	private BukkitTask refreshTimer;
	
	public SelectorMenu(Player player, FileConfiguration config) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", UUID.randomUUID().toString())), 9, player);
		
		this.config = config;
		this.player = player;
		
		this.slots = config.getInt("rows", 6) * 9;
		setSize(slots);
	}
	
	// TODO Re-implement submenu pinging

	@Override
	public void open() {
		addItems();
		
		Cooldown.addCooldown(config.getName() + player.getName(), 0); //Remove cooldown if menu opened successfully
		super.open();
		
		refreshTimer = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
			addItems();
			super.refreshItems();
		}, 1*20, 1*20);
	}

	private void addItems() {		
		for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = config.getConfigurationSection("menu." + key);
			
			String materialString = "STONE";
			int data = 0;
			String name = "";
			List<String> lore = new ArrayList<>();
			int amount = 1;
			boolean enchanted = false;
			
			String action = section.getString("action");
			
			if (action.startsWith("srv")) {
				String serverName = action.substring(4);

				boolean online;
				if (Main.LAST_INFO_TIME.containsKey(serverName)) {
					// If the server has not sent a message for 7 seconds (usually the server sends a message every 5 seconds)
					
					long timeSinceLastPing = System.currentTimeMillis() - Main.LAST_INFO_TIME.get(serverName);
					//System.out.println("[debug] [" + serverName + "] Current time: " + System.currentTimeMillis());
					//System.out.println("[debug] [" + serverName + "] Last ping time: " + Main.LAST_INFO_TIME.get(serverName));
					//System.out.println("[debug] [" + serverName + "] Time since last ping: " + timeSinceLastPing);
					//System.out.println("[debug] [" + serverName + "] Online?: " + (timeSinceLastPing < 7000));
					
					online = timeSinceLastPing < 7000;
				} else {
					//If the server has not sent a message at all it is offline
					//System.out.println("[debug] [" + serverName + "] No ping");
					online = false;
				}
				
				if (online) {
					Map<String, String> placeholders = Main.PLACEHOLDERS.get(serverName);
					
					boolean dynamicMatchFound = false;
					
					if (section.contains("dynamic")) {
						for (String dynamic : section.getConfigurationSection("dynamic").getKeys(false)) {
							String placeholder = dynamic.split(":")[0];
							String result = dynamic.split(":")[1];
							
							if (!placeholders.containsKey(placeholder)) {
								Main.getPlugin().getLogger().warning("Dynamic feature contains rule with placeholder " + placeholder + " which has not been received from the server.");
								continue;
							}
							
							if (placeholders.get(placeholder).equals(result)) {
								//Placeholder result matches with placeholder result in rule
								dynamicMatchFound = true;
								ConfigurationSection dynamicSection = section.getConfigurationSection("dynamic." + dynamic);
								
								materialString = dynamicSection.getString("item");
								data = dynamicSection.getInt("data", 0);
								name = dynamicSection.getString("name");
								lore = dynamicSection.getStringList("lore");
								enchanted = dynamicSection.getBoolean("enchanted", false);
							}
						}
					}
					
					if (!dynamicMatchFound) {
						//No dynamic rule matched, fall back to online
						materialString = section.getString("online.item");
						data = section.getInt("online.data", 0);
						name = section.getString("online.name", "error");
						lore = section.getStringList("online.lore");
						enchanted = section.getBoolean("online.enchanted", false);
					}
					
					for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
						List<String> newLore = new ArrayList<>();
						for (String string : lore) {
							newLore.add(string.replace("{" + placeholder.getKey() + "}", placeholder.getValue()));
						}
						lore = newLore;
						
						name = name.replace(placeholder.getKey(), placeholder.getValue());
					}
				} else {
					//Server is offline
					ConfigurationSection offlineSection = section.getConfigurationSection("offline");
					
					materialString = offlineSection.getString("item");
					data = offlineSection.getInt("data", 0);
					name = offlineSection.getString("name");
					lore = offlineSection.getStringList("lore");
					enchanted = offlineSection.getBoolean("enchanted", false);
				}
				
				// TODO Bring back dynamic item count feature
				amount = section.getInt("item-count", 1);
			} else {
				//Not a server
				materialString = section.getString("item");
				data = section.getInt("data", 0);
				name = section.getString("name", "error");
				lore = section.getStringList("lore");
				enchanted = section.getBoolean("enchanted", false);
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
			
			builder.amount(amount);
			builder.name(Main.PLACEHOLDER_API.parsePlaceholders(player, name));
			builder.lore(Main.PLACEHOLDER_API.parsePlaceholders(player, lore));

			int slot = Integer.valueOf(key);
			ItemStack item = Main.addHideFlags(builder.create());
			
			if (enchanted) item.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
			
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
	}

	@Override
	public boolean onOptionClick(OptionClickEvent event) {		
		int slot = event.getPosition();
		Player player = event.getPlayer();
		
		final boolean permissionsEnabled = Main.getPlugin().getConfig().getBoolean("per-icon-permissions");
		final boolean hasPermission = player.hasPermission("ssx.icon." + config.getName().replace(".yml", "") + "." + slot);
		final boolean hasWildcardPermission = player.hasPermission("ssx.icon." + config.getName().replace(".yml", "") + ".*");
		
		if (permissionsEnabled && !hasPermission && !hasWildcardPermission) {
			return true;
		}
		
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
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("sync player %s %s", player.getName(), Main.PLACEHOLDER_API.parsePlaceholders(player, command)));
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
	
	@Override
	public void onClose(MenuCloseEvent event) {
		refreshTimer.cancel();
	}

}
