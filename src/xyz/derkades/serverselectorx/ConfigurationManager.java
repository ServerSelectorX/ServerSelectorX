package xyz.derkades.serverselectorx;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.derkades.derkutils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages configuration files for server selectors
 */
public class ConfigurationManager {

	private static final File MENU_DIR = new File(Main.getPlugin().getDataFolder(), "menu");
	private Map<String, FileConfiguration> files = new ConcurrentHashMap<>();

	public Collection<FileConfiguration> allFiles() {
		return this.files.values();
	}

	public Set<String> allNames() {
		return this.files.keySet();
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
			try {
				final File defaultConfig = new File(MENU_DIR, "default.yml");
				FileUtils.copyOutOfJar(getClass(), "/default.yml", defaultConfig);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void reload() {
		createDefaultConfig();

		Main.getPlugin().reloadConfig();
		Main.getPlugin().getConfig().setDefaults(new YamlConfiguration());

		this.files = Arrays.stream(MENU_DIR.list())
				.filter(s -> s.endsWith(".yml"))
				.map(s -> MENU_DIR.getPath() + "/" + s)
				.map(File::new)
				.collect(Collectors.toMap(f -> f.getName().replace(".yml", ""),
						YamlConfiguration::loadConfiguration));

		// Initialize variables
		ItemMoveDropCancelListener.DROP_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-drop", false);
		ItemMoveDropCancelListener.MOVE_PERMISSION_ENABLED = getConfig().getBoolean("cancel-item-move", false);
	}

}
