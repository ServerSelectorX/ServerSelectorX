package xyz.derkades.serverselectorx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages configuration files for server selectors
 */
public class ConfigurationManager {

	private static final File MENU_DIR = new File(Main.getPlugin().getDataFolder(), "menu");
	private Map<String, FileConfiguration> files = new ConcurrentHashMap<>();
	
	public Collection<FileConfiguration> getAll() {
		return this.files.values();
	}
	
	public String[] list() {
		return this.files.keySet().toArray(new String[0]);
	}
	
	public FileConfiguration getByName(final String name) {
		return this.files.get(name);
	}
	
	public FileConfiguration getConfig() {
		return Main.getPlugin().getConfig();
	}
	
	private void createDefaultConfig() {
		Main.getPlugin().saveDefaultConfig();
		
		MENU_DIR.mkdirs();
		if (MENU_DIR.listFiles().length == 0) {
			final URL inputUrl = getClass().getResource("/default.yml");
			try {
				final File defaultConfig = new File(MENU_DIR, "default.yml");
				FileUtils.copyURLToFile(inputUrl, defaultConfig);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void reload() {
		createDefaultConfig();

		Main.getPlugin().reloadConfig();

		this.files = Arrays.stream(MENU_DIR.list())
				.filter(s -> s.endsWith(".yml"))
				.map(File::new)
				.collect(Collectors.toMap(f -> f.getName().replace(".yml", ""),
						YamlConfiguration::loadConfiguration));
		
		// Initialize variables
		ItemMoveDropCancelListener.DROP_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-drop", false);
		ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-move", false);
	}

}
