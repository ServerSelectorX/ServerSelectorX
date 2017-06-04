package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.serverselectorx.utils.Config;

public class Main extends JavaPlugin {
	
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
			
		int version = Config.getConfig().getInt("version");
		if (version != 4){
			invalidConfigDisablePlugin();
		}
	}
	
	private static void invalidConfigDisablePlugin(){
		Bukkit.getLogger().log(Level.SEVERE, "************** IMPORTANT **************");
		Bukkit.getLogger().log(Level.SEVERE, "ServerSelectorX: You updated the plugin without deleting the config.");
		Bukkit.getLogger().log(Level.SEVERE, "Please rename config.yml to something else and restart your server.");
		Bukkit.getLogger().log(Level.SEVERE, "If you don't want to redo your config, see resource updates on spigotmc.org for instructions.");
		Bukkit.getLogger().log(Level.SEVERE, "***************************************");
		Bukkit.getServer().getPluginManager().disablePlugin(getPlugin());
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
	
	public static String parseColorCodes(String string){
		return string.replace("&", ChatColor.COLOR_CHAR + "");
	}
	
}
