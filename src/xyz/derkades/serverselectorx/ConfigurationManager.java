package xyz.derkades.serverselectorx;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages configuration files for server selectors
 */
public class ConfigurationManager {

	private final Map<String, FileConfiguration> files = new ConcurrentHashMap<>();
	
	public void loadFiles() {
		files.clear();
		
		for (File file : new File(Main.getPlugin().getDataFolder() + "/menu").listFiles()){
			if (!file.getName().endsWith(".yml"))
				continue;

			String name = file.getName().replace(".yml", "");
			FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			files.put(name, config);
		}
	}
	
	public Collection<FileConfiguration> getAll() {
		return files.values();
	}
	
	public FileConfiguration getByName(String name) {
		return files.get(name);
	}

}
