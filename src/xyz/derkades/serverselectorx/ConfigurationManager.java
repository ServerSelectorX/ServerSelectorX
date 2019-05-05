package xyz.derkades.serverselectorx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages configuration files
 */
public class ConfigurationManager {

	private final Map<String, FileConfiguration> menus = new ConcurrentHashMap<>();
	private FileConfiguration servers;
	private FileConfiguration global;
	private FileConfiguration ssx;
	
	public Map<String, FileConfiguration> getAllMenus() {
		return menus;
	}
	
	public FileConfiguration getMenuByName(String name) {
		return menus.get(name);
	}
	
	public FileConfiguration getServersConfig() {
		return servers;
	}
	
	public FileConfiguration getGlobalConfig() {
		return global;
	}
	
	public FileConfiguration getSSXConfig() {
		return ssx;
	}

	public void reload() {		
		File dataFolder = Main.getPlugin().getDataFolder();
		File menuFolder = new File(dataFolder, "menu");
		menuFolder.mkdirs(); //will create data folder as well
		
		if (menuFolder.listFiles().length == 0){
			// Save default.yml file if menu folder is empty
			saveDefaultAndLoad("default-selector", new File(menuFolder, "default.yml"));
		}
		
		servers = saveDefaultAndLoad("servers", new File(dataFolder, "servers.yml"));
		global = saveDefaultAndLoad("global", new File(dataFolder, "global.yml"));
		ssx = saveDefaultAndLoad("ssx", new File(dataFolder, "ssx.yml"));
		
		//Initialize variables
		ItemMoveDropCancelListener.DROP_PERMISSION_ENABLED = global.getBoolean("cancel-item-drop", false);
		ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED = global.getBoolean("cancel-item-move", false);
		
		menus.clear();
		for (File file : menuFolder.listFiles()) {
			if (!(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))){
				Main.getPlugin().getLogger().warning("Skipped non-yml file " + file.getPath());
				continue;
			}
			
			menus.put(file.getName().replace(".yml", "").replace(".yaml", ""), YamlConfiguration.loadConfiguration(file));
		}
	}
	
	private FileConfiguration saveDefaultAndLoad(String name, File destination) {
		if (!destination.exists()) {
			URL inputUrl = getClass().getResource("/xyz/derkades/serverselectorx/" + name + ".yml");
			try {
				FileUtils.copyURLToFile(inputUrl, destination);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		
		return YamlConfiguration.loadConfiguration(destination);
	}

}
