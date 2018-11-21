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

	public void reload() {		
		File dataFolder = Main.getPlugin().getDataFolder();
		File menuFolder = new File(dataFolder, "menu");
		menuFolder.mkdirs();

		if (menuFolder.listFiles().length == 0){
			// Save default.yml file if menu folder is empty
			saveDefaultAndLoad("default-selector", new File(menuFolder, "default.yml"));
		}
		
		Main.getPlugin().reloadConfig();
		
		loadServersConfig();

		menus.clear();
		for (File file : new File(Main.getPlugin().getDataFolder() + File.separator + "menu").listFiles()){
			if (!file.getName().endsWith(".yml"))
				continue;

			String name = file.getName().replace(".yml", "");
			FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			menus.put(name, config);
		}
		
		//Initialize variables
		ItemMoveDropCancelListener.DROP_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-drop", false);
		ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-move", false);
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
