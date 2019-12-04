package xyz.derkades.serverselectorx.configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import xyz.derkades.serverselectorx.Main;

/**
 * Manages configuration files
 */
public class ConfigurationManager {

	private final Map<String, FileConfiguration> commands = new ConcurrentHashMap<>();
	private final Map<String, FileConfiguration> menus = new ConcurrentHashMap<>();
	private final Map<String, FileConfiguration> items = new ConcurrentHashMap<>();
	private FileConfiguration global;
	private FileConfiguration ssx;

	public ConfigurationManager() {
		this.reload();
	}

	public Map<String, FileConfiguration> getCommands() {
		return this.commands;
	}

	public Map<String, FileConfiguration> getMenus() {
		return this.menus;
	}

	public Map<String, FileConfiguration> getItems(){
		return this.items;
	}

	public FileConfiguration getGlobalConfig() {
		return this.global;
	}

	public FileConfiguration getSSXConfig() {
		return this.ssx;
	}

	public void reload() {
		final File dataFolder = Main.getPlugin().getDataFolder();

		final boolean generate = !new File(dataFolder, "ssx.yml").exists();

		this.global = this.saveDefaultAndLoad("global", new File(dataFolder, "global.yml"));
		this.ssx = this.saveDefaultAndLoad("ssx", new File(dataFolder, "ssx.yml"));

		final File menuFolder = new File(dataFolder, "menu");
		menuFolder.mkdirs();

		this.menus.clear();
		if (menuFolder.listFiles().length == 0 && generate) {
			// Save default.yml file if menu folder is empty
			this.menus.put("default", this.saveDefaultAndLoad("default-selector", new File(menuFolder, "default.yml")));
		} else {
			// Load files from directory
			for (final File file : this.getFilesFromFolder(menuFolder)) {
				this.menus.put(file.getName().replace(".yml", "").replace(".yaml", ""), YamlConfiguration.loadConfiguration(file));
			}
		}

		final File itemFolder = new File(dataFolder, "item");
		itemFolder.mkdirs();

		this.items.clear();
		if (itemFolder.listFiles().length == 0 && generate) {
			// Save serverselector.yml file if menu folder is empty
			this.items.put("default", this.saveDefaultAndLoad("default-item", new File(itemFolder, "serverselector.yml")));
		} else {
			// Load files from directory
			for (final File file : this.getFilesFromFolder(itemFolder)) {
				this.items.put(file.getName().replace(".yml", "").replace(".yaml", ""), YamlConfiguration.loadConfiguration(file));
			}
		}

		final File commandFolder = new File(dataFolder, "command");
		commandFolder.mkdirs();

		this.commands.clear();
		if (commandFolder.listFiles().length == 0 && generate) {
			// Save menu.yml file if menu folder is empty
			this.commands.put("servers", this.saveDefaultAndLoad("default-command", new File(commandFolder, "servers.yml")));
		} else {
			// Load files from directory
			for (final File file : this.getFilesFromFolder(commandFolder)) {
				this.commands.put(file.getName().replace(".yml", "").replace(".yaml", ""), YamlConfiguration.loadConfiguration(file));
			}
		}
	}

	private FileConfiguration saveDefaultAndLoad(final String name, final File destination) {
		if (!destination.exists()) {
			final URL inputUrl = this.getClass().getResource("/" + name + ".yml");
			try {
				FileUtils.copyURLToFile(inputUrl, destination);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return YamlConfiguration.loadConfiguration(destination);
	}

	private File[] getFilesFromFolder(final File folder) {
		return folder.listFiles((f) -> f.getName().endsWith(".yml") || f.getName().endsWith(".yaml"));
	}

}
