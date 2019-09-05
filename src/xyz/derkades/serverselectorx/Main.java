package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.placeholders.Placeholders;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersDisabled;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersEnabled;

public class Main extends JavaPlugin {
	
	public static Placeholders PLACEHOLDER_API;
	
	public static Map<String, Map<String, Object>> SERVER_PLACEHOLDERS = new HashMap<>();

	private static ConfigurationManager configurationManager;
	
	private static JavaPlugin plugin;
	
	public static JavaPlugin getPlugin(){
		return plugin;
	}
	
	@Override
	public void onEnable(){
		plugin = this;
		
		configurationManager = new ConfigurationManager();
		configurationManager.reloadAll();

		//Register listeners
		Bukkit.getPluginManager().registerEvents(new SelectorOpenListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemMoveDropCancelListener(), this);
		
		final List<String> offHandVersions = Arrays.asList("1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15");
		for (final String version : offHandVersions) {
			if (Bukkit.getBukkitVersion().contains(version)) {
				Bukkit.getPluginManager().registerEvents(new OffHandMoveCancel(), this);
			}
		}
		
		//Register messaging channels
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		new PingServersBackground().start();
		
		//Register command
		getCommand("serverselectorx").setExecutor(new ReloadCommand());
		
		//Start bStats
		Stats.initialize();
		
		//Register custom selector commands
		registerCommands();
		
		//Check if PlaceHolderAPI is installed
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			Main.PLACEHOLDER_API = new PlaceholdersEnabled();
			getLogger().log(Level.INFO, "PlaceholderAPI is found. Placeholders will work!");
		} else {
			Main.PLACEHOLDER_API = new PlaceholdersDisabled();
			getLogger().log(Level.INFO, "PlaceholderAPI is not installed. The plugin will still work.");
		}
		
		getLogger().info("Thank you for using ServerSelectorX. If you enjoy using this plugin, please consider buying the premium version. It has more features and placeholders update instantly. https://github.com/ServerSelectorX/ServerSelectorX/wiki/Premium");
	}
	
	/**
	 * Registers all custom commands by going through all menu files and adding commands
	 */
	private void registerCommands(){
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
	
			bukkitCommandMap.setAccessible(true);
			final CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
			
			for (final FileConfiguration config : Main.getServerSelectorConfigurationFiles()){
				final String commandName = config.getString("command");
				
				if (commandName == null || commandName.equalsIgnoreCase("none")) {
					continue;
				}
				
				commandMap.register("ssx-custom", new Command(commandName){
	
					@Override
					public boolean execute(final CommandSender sender, final String label, final String[] args) {
						if (sender instanceof Player){
							final Player player = (Player) sender;
							Main.openSelector(player, config);
						}
						return true;
					}
					
				});
	
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	@Deprecated
	public static FileConfiguration getSelectorConfigurationFile(final String name){
		final File file = new File(Main.getPlugin().getDataFolder() + "/menu", name + ".yml");
		if (file.exists()){
			return YamlConfiguration.loadConfiguration(file);
		} else {
			return null;
		}
	}
	
	@Deprecated
	public static List<FileConfiguration> getServerSelectorConfigurationFiles(){
		final List<FileConfiguration> configs = new ArrayList<>();
		for (final File file : new File(Main.getPlugin().getDataFolder() + "/menu").listFiles()){
			if (!file.getName().endsWith(".yml"))
				continue;

			configs.add(YamlConfiguration.loadConfiguration(file));
		}
		return configs;
	}
	
	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}
	
	public static void openSelector(final Player player, final FileConfiguration config) {
		final long cooldown = Cooldown.getCooldown(config.getName() + player.getName());
		if (cooldown > 0) {
			String cooldownMessage = Main.getPlugin().getConfig().getString("cooldown-message", "&cYou cannot use this yet, please wait {x} seconds.");
			cooldownMessage = cooldownMessage.replace("{x}", String.valueOf((cooldown / 1000) + 1));
			cooldownMessage = Colors.parseColors(cooldownMessage);
			if (!(cooldownMessage.equals("") || cooldownMessage.equals(" "))) { //Do not send message if message is an empty string
				player.sendMessage(cooldownMessage);
			}
			
			return;
		}
		
		final long cooldownDuration = Main.getPlugin().getConfig().getLong("selector-open-cooldown", 0);	
		if (cooldownDuration >= 1000) {
			Cooldown.addCooldown(config.getName() + player.getName(), cooldownDuration);
		}
			
		// Play sound
		final String soundString = Main.getPlugin().getConfig().getString("selector-open-sound");
		if (soundString != null && !soundString.equals("NONE")) {
			try {
				final Sound sound = Sound.valueOf(soundString);
				player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
			} catch (final IllegalArgumentException e) {
				Main.getPlugin().getLogger().log(Level.WARNING, "A sound with the name " + soundString
						+ " could not be found. Make sure that it is the right name for your server version.");
			}
		}

		new SelectorMenu(player, config).open();
	}
	
	public static void teleportPlayerToServer(final Player player, final String server){
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0) {
			return;
		}
		
		Cooldown.addCooldown("servertp" + player.getName() + server, 1000);
		
		if (Main.getPlugin().getConfig().getBoolean("server-teleport-message-enabled", false)){
			if (Main.getPlugin().getConfig().getBoolean("chat-clear", false)){
				for (int i = 0; i < 150; i++) {
					player.sendMessage("");
				}
			}
			
			final String message = Colors.parseColors(Main.getPlugin().getConfig().getString("server-teleport-message", "error"));
			player.sendMessage(message.replace("{x}", server));
		}

		try (
				ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
				DataOutputStream dos = new DataOutputStream(baos)
			){
			
	        dos.writeUTF("Connect");
	        dos.writeUTF(server);
	        player.sendPluginMessage(getPlugin(), "BungeeCord", baos.toByteArray());
		} catch (final IOException e){
			e.printStackTrace();
		}
	}

}
