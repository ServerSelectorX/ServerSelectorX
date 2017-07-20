package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.placeholders.Placeholders;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersDisabled;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersEnabled;
import xyz.derkades.serverselectorx.utils.Cache;

public class Main extends JavaPlugin {
	
	private static final int CONFIG_VERSION = 6;
	
	private static final List<String> COOLDOWN = new ArrayList<String>();
	
	public static final int GLOWING_ENCHANTMENT_ID = 96;
	
	public static Placeholders PLACEHOLDER_API;
	
	private static Plugin plugin;
	
	public static Plugin getPlugin(){
		return plugin;
	}
	
	@Override
	public void onEnable(){
		plugin = this;

		//Register listeners
		Bukkit.getPluginManager().registerEvents(new SelectorOpenListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemMoveDropCancelListener(), this);
		
		//Register outgoing channel
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		//Register command
		getCommand("serverselectorx").setExecutor(new ReloadCommand());
		
		//Start bStats
		new Metrics(this);
		
		//Check if config is up to date
		int version = getConfig().getInt("version");
		if (version != CONFIG_VERSION){
			Logger logger = super.getLogger();
			logger.log(Level.SEVERE, "************** IMPORTANT **************");
			logger.log(Level.SEVERE, "You updated the plugin without deleting the config.");
			logger.log(Level.SEVERE, "Please rename config.yml to something else and restart your server.");
			logger.log(Level.SEVERE, "If you don't want to redo your config, see resource updates on spigotmc.org for instructions.");
			logger.log(Level.SEVERE, "***************************************");
			getServer().getPluginManager().disablePlugin(this);
		}
		
		//Register custom selector commands
		registerCommands();
		
		//Register glowing enchantment
		registerEnchantment();
		
		//Check if placeholderapi is installed
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			Main.PLACEHOLDER_API = new PlaceholdersEnabled();
			getLogger().log(Level.INFO, "PlaceholderAPI is found. Placeholders will work!");
		} else {
			Main.PLACEHOLDER_API = new PlaceholdersDisabled();
			getLogger().log(Level.INFO, "PlaceholderAPI is not installed. The plugin will still work.");
		}
		
		//Periodically clean cache
		new BukkitRunnable(){
			public void run(){
				Cache.cleanCache();
			}
		}.runTaskTimer(this, 60*20, 60*20);
	}
	
	public void reloadConfig(){	
		//Setup config
		super.saveDefaultConfig();
		
		//Reload config
		super.reloadConfig();
	
		//Copy custom config
		File file = new File(this.getDataFolder() + "/menu", "default.yml");
		if (!file.exists()){
			URL inputUrl = getClass().getResource("/xyz/derkades/serverselectorx/default-selector.yml");
			try {
				FileUtils.copyURLToFile(inputUrl, file);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		
		//Initialise variables
		ItemMoveDropCancelListener.DROP_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-drop", false);
		ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-move", false);
	}
	
	private static void registerCommands(){
		for (FileConfiguration config : Main.getServerSelectorConfigurationFiles()){
			String commandName = config.getString("command");
			if (commandName == null || commandName.equalsIgnoreCase("none"));
			
			Command command = new Command(commandName){

				@Override
				public boolean execute(CommandSender sender, String label, String[] args) {
					if (sender instanceof Player){
						Player player = (Player) sender;
						Main.openSelector(player, config);
					}
					return true;
				}
				
			};
			
			try {
				final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

				bukkitCommandMap.setAccessible(true);
				CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

				commandMap.register("ssx-custom", command);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void registerEnchantment(){
		try {
			Field f = Enchantment.class.getDeclaredField("acceptingNew");
			f.setAccessible(true);
			f.set(null, true);
			
			EnchantmentWrapper.registerEnchantment(new GlowEnchantment());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e){
			//Already registered
		}
	}
	
	public static FileConfiguration getSelectorConfigurationFile(String name){
		File file = new File(Main.getPlugin().getDataFolder() + "/menu", name + ".yml");
		if (file.exists()){
			return YamlConfiguration.loadConfiguration(file);
		} else {
			return null;
		}
	}
	
	public static List<FileConfiguration> getServerSelectorConfigurationFiles(){
		List<FileConfiguration> configs = new ArrayList<>();
		for (File file : new File(Main.getPlugin().getDataFolder() + "/menu").listFiles()){
			configs.add(YamlConfiguration.loadConfiguration(file));
		}
		return configs;
	}
	
	public static void openSelector(Player player, FileConfiguration config) {
		long cooldown = Cooldown.getCooldown(config.getName() + player.getName());
		if (cooldown > 0) {
			String cooldownMessage = Main.getPlugin().getConfig().getString("cooldown-message", "&cYou cannot use this yet, please wait {x} seconds.");
			cooldownMessage = cooldownMessage.replace("{x}", String.valueOf((cooldown / 1000) + 1));
			cooldownMessage = Colors.parseColors(cooldownMessage);
			if (!(cooldownMessage.equals("") || cooldownMessage.equals(" "))) { //Do not send message if message is an empty string
				player.sendMessage(cooldownMessage);
			}
			
			return;
		}
		
		long cooldownDuration = Main.getPlugin().getConfig().getLong("selector-open-cooldown", 1000);		
		Cooldown.addCooldown(config.getName() + player.getName(), cooldownDuration);
		
		final boolean permissionsEnabled = Main.getPlugin().getConfig().getBoolean("permissions-enabled");
		final boolean hasPermission = player.hasPermission("ssx.use." + config.getName().replace(".yml", ""));
		if (!permissionsEnabled || hasPermission){
			
			//Play sound
			String soundString = Main.getPlugin().getConfig().getString("selector-open-sound");
			if (soundString != null && !soundString.equals("NONE")){
				try {
					Sound sound = Sound.valueOf(soundString);
					player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
				} catch (IllegalArgumentException e){
					Main.getPlugin().getLogger().log(Level.WARNING, "A sound with the name " + soundString + " could not be found. Make sure that it is the right name for your server version.");
				}
			}
			
			final int rows = config.getInt("rows");
			final String title = Colors.parseColors(config.getString("title"));
			
			new SelectorMenu(title, rows * 9, player, config).open();
		} else if (config.getBoolean("no-permission-message-enabled", false)) {
			player.sendMessage(config.getString("no-permission-message"));
			return;
		}
	}
	
	public static void teleportPlayerToServer(final Player player, final String server){
		if (Main.getPlugin().getConfig().getBoolean("server-teleport-message-enabled", false)){
			if (Main.getPlugin().getConfig().getBoolean("chat-clear", false)){
				for (int i = 0; i < 20; i++){ //Send just 20 lines, because clearing the complete chat is unnecessary.
					player.sendMessage("");
				}
			}
			
			String message = Colors.parseColors(Main.getPlugin().getConfig().getString("server-teleport-message", "error"));
			player.sendMessage(message.replace("{x}", server));
		}
		
		String playerName = player.getName();
		if (COOLDOWN.contains(playerName)){
			return;
		} else {
			COOLDOWN.add(playerName);
			new BukkitRunnable(){
				public void run(){
					if (COOLDOWN.contains(playerName))
						COOLDOWN.remove(playerName);
				}
			}.runTaskLater(Main.getPlugin(), 1*20);
		}

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        DataOutputStream dos = new DataOutputStream(baos);
	        dos.writeUTF("Connect");
	        dos.writeUTF(server);
	        player.sendPluginMessage(getPlugin(), "BungeeCord", baos.toByteArray());
	        baos.close();
	        dos.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

}
