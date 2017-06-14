package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {
	
	private static final int CONFIG_VERSION = 4;
	
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
			URL inputUrl = getClass().getResource("default-selector.yml");
			try{
				FileUtils.copyURLToFile(inputUrl, file);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	
	public static void teleportPlayerToServer(final Player player, final String server){
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
	
	@Deprecated
	public static String parseColorCodes(String string){
		return string.replace("&", ChatColor.COLOR_CHAR + "");
	}
	
}
