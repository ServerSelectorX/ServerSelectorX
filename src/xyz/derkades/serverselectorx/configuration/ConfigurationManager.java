package xyz.derkades.serverselectorx.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import xyz.derkades.derkutils.FileUtils;
import xyz.derkades.serverselectorx.Main;

/**
 * Manages configuration files
 */
public class ConfigurationManager {

	public Map<String, FileConfiguration> commands;
	public Map<String, FileConfiguration> menus;
	public Map<String, FileConfiguration> items;

	public FileConfiguration api;
	public FileConfiguration chatcomponents;
	public FileConfiguration inventory;
	public FileConfiguration join;
	public FileConfiguration misc;
	public FileConfiguration sync;

	public void reload() throws IOException {
		final File dir = Main.getPlugin().getDataFolder();
		dir.mkdirs();

		// First copy all files to the config directory

		final String[] files = {
				"api.yml",
				"chatcomponents.yml",
				"inventory.yml",
				"join.yml",
				"misc.yml",
				"sync.yml",
		};

		final File configDir = new File(dir, "config");
		configDir.mkdirs();

		for (final String fileName : files) {
			final File file = new File(configDir, fileName);
			if (!file.exists()) {
				FileUtils.copyOutOfJar(this.getClass(), "/config/" + fileName, file);
			}

			if (fileName.equals("api.yml")) {
				this.api = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("chatcomponents.yml")) {
				this.chatcomponents = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("inventory.yml")) {
				this.inventory = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("join.yml")) {
				this.join = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("misc.yml")) {
				this.misc = YamlConfiguration.loadConfiguration(file);
			} else if (fileName.equals("sync.yml")) {
				this.sync = YamlConfiguration.loadConfiguration(file);
			} else {
				throw new AssertionError();
			}
		}

		final File commandDir = new File(dir, "command");
		if (!commandDir.exists()) {
			commandDir.mkdir();
			FileUtils.copyOutOfJar(this.getClass(), "/command.yml",
					new File(commandDir, "servers.yml"));
		}

		final File itemDir = new File(dir, "item");
		if (!itemDir.exists()) {
			itemDir.mkdir();
			FileUtils.copyOutOfJar(this.getClass(), "/item.yml",
					new File(itemDir, "compass.yml"));
		}

		final File menuDir = new File(dir, "menu");
		if (!menuDir.exists()) {
			menuDir.mkdir();
			FileUtils.copyOutOfJar(this.getClass(), "/menu.yml",
					new File(menuDir, "serverselector.yml"));
		}

		this.commands = new ConcurrentHashMap<>();
		for (final File file : commandDir.listFiles()) {
			if (file.getName().endsWith(".yml")) {
				this.commands.put(configName(file), YamlConfiguration.loadConfiguration(file));
			}
		}

		this.items = new ConcurrentHashMap<>();
		for (final File file : itemDir.listFiles()) {
			if (file.getName().endsWith(".yml")) {
				this.items.put(configName(file), YamlConfiguration.loadConfiguration(file));
			}
		}

		this.menus = new ConcurrentHashMap<>();
		for (final File file : menuDir.listFiles()) {
			if (file.getName().endsWith(".yml")) {
				this.menus.put(configName(file), YamlConfiguration.loadConfiguration(file));
			}
		}
	}

	private static String configName(final File file) {
		return file.getName().substring(0, file.getName().length() - 4);
	}

}
