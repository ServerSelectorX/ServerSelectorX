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
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.derkutils.bukkit.Colors;

public class Main extends JavaPlugin {
	
	private static final int CONFIG_VERSION = 5;
	
	private static final List<String> COOLDOWN = new ArrayList<String>();
	
	private static Plugin plugin;
	
	public static Plugin getPlugin(){
		return plugin;
	}
	
	@Override
	public void onEnable(){
		plugin = this;
		
		//Setup config
		super.saveDefaultConfig();
		
		//Register listeners
		Bukkit.getPluginManager().registerEvents(new SelectorOpenListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);
		
		//Register outgoing channel
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		//Register command
		getCommand("serverselectorx").setExecutor(new ReloadCommand());
		
		//Start bStats
		new Metrics(this);
			
		int version = getConfig().getInt("version");
		if (version != CONFIG_VERSION){
			Logger logger = super.getLogger();
			logger.log(Level.SEVERE, "************** IMPORTANT **************");
			logger.log(Level.SEVERE, "You updated the plugin without deleting the config.");
			logger.log(Level.SEVERE, "Please rename config.yml to something else and restart your server.");
			logger.log(Level.SEVERE, "If you don't want to redo your config, see resource updates on spigotmc.org for instructions.");
			logger.log(Level.SEVERE, "***************************************");
			getServer().getPluginManager().disablePlugin(getPlugin());
		}
		
		File file = new File(this.getDataFolder() + "/menu", "default.yml");
		if (!file.exists()){
			URL inputUrl = getClass().getResource("/xyz/derkades/serverselectorx/default-selector.yml");
			try {
				FileUtils.copyURLToFile(inputUrl, file);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		
		for (FileConfiguration config : Main.getServerSelectorConfigurationFiles()){
			String commandName = config.getString("command");
			if (commandName == null || commandName.equalsIgnoreCase("none"));
			
			Command command = new Command(commandName){

				@Override
				public boolean execute(CommandSender sender, String label, String[] args) {
					if (sender instanceof Player){
						Player player = (Player) sender;
						final int rows = config.getInt("rows");
						final String title = Colors.parseColors(config.getString("title"));
						new SelectorMenu(title, rows * 9, player, config).open();
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
