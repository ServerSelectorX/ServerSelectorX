package xyz.derkades.serverselectorx.configuration;

import java.io.File;
import java.io.IOException;
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

	public Map<String, FileConfiguration> commands;
	public Map<String, FileConfiguration> menus;
	public Map<String, FileConfiguration> items;

	public FileConfiguration api;
	public FileConfiguration effects;
	public FileConfiguration inventory;
	public FileConfiguration misc;
	public FileConfiguration sync;

	public void reload() throws IOException {
		final File dir = Main.getPlugin().getDataFolder();

		// First copy all files to the config directory

		// files needed to be listed manually because in
		// java we can't list files in a jar
		final String[] files = {
				"api.yml",
				"effects.yml",
				"inventory.yml",
				"misc.yml",
				"sync.yml",
		};

		final File configDir = new File(dir, "config");
		boolean generateMenuItemCommand = false;
		if (!configDir.exists()) {
			configDir.mkdirs();
			generateMenuItemCommand = true;
		}

		for (final String fileName : files) {
			final File file = new File(configDir, fileName);
			if (!file.exists()) {
				FileUtils.copyURLToFile(this.getClass().getResource("/config/" + file), file);
			}

			if (fileName.equals("api.yml")) {
				this.api = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("effects.yml")) {
				this.effects = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("inventory.yml")) {
				this.inventory = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("misc.yml")) {
				this.misc = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("sync.yml")) {
				this.sync = YamlConfiguration.loadConfiguration(file);
			} else {
				throw new AssertionError();
			}
		}

		final File commandDir = new File(dir, "command");
		final File itemDir = new File(dir, "item");
		final File menuDir = new File(dir, "menu");

		if (generateMenuItemCommand &&
				!commandDir.exists() &&
				!itemDir.exists() &&
				!menuDir.exists()) {

			commandDir.mkdirs();
			itemDir.mkdirs();
			menuDir.mkdirs();

			FileUtils.copyURLToFile(this.getClass().getResource("/command.yml"),
					new File(commandDir, "servers.yml"));
			FileUtils.copyURLToFile(this.getClass().getResource("/item.yml"),
					new File(itemDir, "compass.yml"));
			FileUtils.copyURLToFile(this.getClass().getResource("/menu.yml"),
					new File(menuDir, "default.yml"));
		}

		this.commands = new ConcurrentHashMap<>();
		for (final File file : commandDir.listFiles()) {
			if (file.getName().endsWith(".yml")) {
				this.commands.put(configName(file), YamlConfiguration.loadConfiguration(file));
			}
		}

		this.items = new ConcurrentHashMap<>();
		for (final File file : commandDir.listFiles()) {
			if (file.getName().endsWith(".yml")) {
				this.items.put(configName(file), YamlConfiguration.loadConfiguration(file));
			}
		}

		this.menus = new ConcurrentHashMap<>();
		for (final File file : commandDir.listFiles()) {
			if (file.getName().endsWith(".yml")) {
				this.menus.put(configName(file), YamlConfiguration.loadConfiguration(file));
			}
		}
	}

	private static String configName(final File file) {
		return file.getName().substring(0, file.getName().length() - 4);
	}

}
