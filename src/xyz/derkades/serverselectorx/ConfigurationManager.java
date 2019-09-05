package xyz.derkades.serverselectorx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages configuration files for server selectors
 */
public class ConfigurationManager {

	private final Map<String, FileConfiguration> files = new ConcurrentHashMap<>();
	
	public Collection<FileConfiguration> getAll() {
		return this.files.values();
	}
	
	public FileConfiguration getByName(final String name) {
		return this.files.get(name);
	}
	
	public FileConfiguration getConfig() {
		return Main.getPlugin().getConfig();
	}
	
	public void reloadAll() {
		
		//Create default configuration files
		
		Main.getPlugin().saveDefaultConfig();
		
		final File dir = new File(Main.getPlugin().getDataFolder() + File.separator + "menu");
		dir.mkdirs();
		if (dir.listFiles().length == 0){
			final URL inputUrl = getClass().getResource("/default.yml");
			try {
				final File defaultConfig = new File(dir, "default.yml");
				FileUtils.copyURLToFile(inputUrl, defaultConfig);
			} catch (final IOException e){
				e.printStackTrace();
			}
		}
		
		//Reload configuration files
		
		Main.getPlugin().reloadConfig();
		
		this.files.clear();
		for (final File file : new File(Main.getPlugin().getDataFolder() + File.separator + "menu").listFiles()){
			if (!file.getName().endsWith(".yml"))
				continue;

			final String name = file.getName().replace(".yml", "");
			final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			this.files.put(name, config);
		}
		
		//Initialize variables
		ItemMoveDropCancelListener.DROP_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-drop", false);
		ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-move", false);
	}

}
