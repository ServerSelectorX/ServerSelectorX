package xyz.derkades.serverselectorx.configuration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.FileUtils;
import xyz.derkades.serverselectorx.Main;

/**
 * Manages configuration files
 */
public class ConfigurationManager {

	private static final File CONFIG_DIR = new File(Main.getPlugin().getDataFolder(), "config");

	private enum StandardConfigFile {

		API(new File(CONFIG_DIR, "api.yml")),
		INVENTORY(new File(CONFIG_DIR, "inventory.yml")),
		JOIN(new File(CONFIG_DIR, "join.yml")),
		MISC(new File(CONFIG_DIR, "misc.yml")),
		SYNC(new File(CONFIG_DIR, "sync.yml"));

		private final @NotNull File file;

		StandardConfigFile(final @NotNull File file){
			this.file = file;
		}

		private @NotNull YamlConfiguration copyLoad() throws IOException {
			if (!this.file.exists()) {
				FileUtils.copyOutOfJar(this.getClass(), "/config/" + this.file.getName(), this.file);
			}
			return YamlConfiguration.loadConfiguration(this.file);
		}
	}

	private enum MultiConfigFile {

		COMMAND(new File(Main.getPlugin().getDataFolder(), "command"), "/command.yml", "servers.yml"),
		ITEM(new File(Main.getPlugin().getDataFolder(), "item"), "/item.yml", "compass.yml"),
		MENU(new File(Main.getPlugin().getDataFolder(), "menu"), "/menu.yml", "serverselector.yml");

		private final @NotNull File dir;
		private final @NotNull String defaultFileInJar;
		private final @NotNull String defaultFileOutsideJar;

		MultiConfigFile(final @NotNull File dir, final @NotNull String defaultFileInJar,
						final @NotNull String defaultFileOutsideJar) {
			this.dir = dir;
			this.defaultFileInJar = defaultFileInJar;
			this.defaultFileOutsideJar = defaultFileOutsideJar;
		}

		private void copyLoad(final @NotNull Map<String, FileConfiguration> dest) throws IOException {
			if (!this.dir.exists()) {
				this.dir.mkdir();
				FileUtils.copyOutOfJar(this.getClass(), this.defaultFileInJar,
						new File(this.dir, this.defaultFileOutsideJar));
			}

			File[] files = this.dir.listFiles();
			if (files != null) {
				for (final File file : files) {
					if (file.getName().endsWith(".yml")) {
						dest.put(configName(file), YamlConfiguration.loadConfiguration(file));
					}
				}
			}
		}

		private static String configName(final File file) {
			return file.getName().substring(0, file.getName().length() - 4);
		}

	}

	private final Map<StandardConfigFile, FileConfiguration> STANDARD_FILES = new EnumMap<>(StandardConfigFile.class);
	private final Map<MultiConfigFile, Map<String, FileConfiguration>> MULTI_FILES = new EnumMap<>(MultiConfigFile.class);

	public void reload() throws IOException {
		final File dir = Main.getPlugin().getDataFolder();
		final File configDir = new File(dir, "config");
		configDir.mkdirs();

		// First copy all files to the config directory

		synchronized(this.STANDARD_FILES) {
			for (final StandardConfigFile file : StandardConfigFile.values()) {
				this.STANDARD_FILES.put(file, file.copyLoad());
			}
		}

		synchronized(this.MULTI_FILES) {
			for (final MultiConfigFile file : MultiConfigFile.values()) {
				final Map<String, FileConfiguration> files = new HashMap<>();
				file.copyLoad(files);
				this.MULTI_FILES.put(file, files);
			}
		}
	}

	public @Nullable FileConfiguration getCommandConfiguration(final String name) {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.COMMAND).get(name);
		}
	}

	public @NotNull Set<String> listCommandConfigurations() {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.COMMAND).keySet();
		}
	}

	public @Nullable FileConfiguration getItemConfiguration(final String name) {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.ITEM).get(name);
		}
	}

	public @NotNull Set<String> listItemConfigurations() {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.ITEM).keySet();
		}
	}

	public @Nullable FileConfiguration getMenuConfiguration(final String name) {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.MENU).get(name);
		}
	}

	public @NotNull Set<String> listMenuConfigurations() {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.MENU).keySet();
		}
	}

	public @NotNull FileConfiguration getApiConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.API);
		}
	}

	public @NotNull FileConfiguration getInventoryConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.INVENTORY);
		}
	}

	public @NotNull FileConfiguration getJoinConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.JOIN);
		}
	}

	public @NotNull FileConfiguration getMiscConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.MISC);
		}
	}

	public @NotNull FileConfiguration getSyncConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.SYNC);
		}
	}


}
