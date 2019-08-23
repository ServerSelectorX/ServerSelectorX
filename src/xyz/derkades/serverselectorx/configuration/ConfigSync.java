package xyz.derkades.serverselectorx.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.derkades.serverselectorx.Main;

public class ConfigSync {

	public ConfigSync() {
		final ConfigurationSection syncConfig = Main.getConfigurationManager().getSSXConfig().getConfigurationSection("config-sync");

		if (!syncConfig.getBoolean("enabled", false))
			return;

		// Run 1 second after server startup, then every hour
		Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), new Task(), 20, 60*60*20);
	}

	private static class Task implements Runnable {

		@Override
		public void run() {
			final ConfigurationSection syncConfig = Main.getConfigurationManager().getSSXConfig().getConfigurationSection("config-sync");

			final Logger logger = Main.getPlugin().getLogger();

			logger.info("Starting config sync");

			final String address = syncConfig.getString("address");
			final List<String> whitelist = syncConfig.getStringList("whitelist");

			URL url;
			try {
				url = new URL("http://" + address + "/config");
			} catch (final MalformedURLException e) {
				logger.severe("The address you entered seems to be incorrectly formatted.");
				logger.severe("It must be formatted like this: 173.45.16.208:8888");
				return;
			}

			Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
				//logger.info("Making request to " + url.toString());
				boolean error = false;
				String jsonOutput = null;
				try {
					final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					final BufferedReader streamReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
					final StringBuilder responseBuilder = new StringBuilder();
					String temp;
					while ((temp = streamReader.readLine()) != null) {
						responseBuilder.append(temp);
					}
					jsonOutput = responseBuilder.toString();
				} catch (final IOException e) {
					e.printStackTrace();
					error = true;
				}

				if (error) {
					logger.severe("An error occured while making the request. Are you using the right IP and port?");
					return;
				}

				if (jsonOutput == null) {
					// it should only ever be null if error == true
					logger.severe("Json output null. Report issue to developer");
					return;
				}

				final File pluginDirectory = Main.getPlugin().getDataFolder();

				final File serversYml = new File(pluginDirectory, "servers.yml");
				final File globalYml = new File(pluginDirectory, "global.yml");
				final File menuDirectory = new File(pluginDirectory, "menu");
				final File itemDirectory = new File(pluginDirectory, "item");

				final JsonParser parser = new JsonParser();
				final JsonObject json = parser.parse(jsonOutput).getAsJsonObject();

				logger.info("Writing to disk..");

				try {
					if (whitelist.contains("servers")) {
						serversYml.delete();
						final FileConfiguration config = new YamlConfiguration();
						config.loadFromString(json.get("servers").getAsString());
						config.save(serversYml);
						logger.info("Replaced servers.yml");
					}

					if (whitelist.contains("global")) {
						globalYml.delete();
						final FileConfiguration config = new YamlConfiguration();
						config.loadFromString(json.get("global").getAsString());
						config.save(globalYml);
						logger.info("Replaced global.yml");
					}

					if (whitelist.contains("menu:all")) {
						Arrays.asList(menuDirectory.listFiles()).forEach(File::delete);

						final JsonObject menuFilesJson = json.get("menu").getAsJsonObject();
						for (final Entry<String, JsonElement> menuJson : menuFilesJson.entrySet()) {
							final FileConfiguration config = new YamlConfiguration();
							config.loadFromString(menuJson.getValue().getAsString());
							config.save(new File(menuDirectory, menuJson.getKey() + ".yml"));
							logger.info("Downloaded menu/" + menuJson.getKey() + ".yml");
						}
					} else {
						for (final String string : whitelist) {
							if (!string.startsWith("menu:")) {
								continue;
							}

							final String menuName = string.substring(5);
							if (!json.get("menu").getAsJsonObject().has(menuName)) {
								logger.warning("Menu with name '" + menuName + "' is in the config sync whitelist, but was not sent by the server.");
								continue;
							}

							final JsonElement menuJson = json.get("menu").getAsJsonObject().get(menuName);
							final FileConfiguration config = new YamlConfiguration();
							config.loadFromString(menuJson.getAsString());
							config.save(new File(menuDirectory, menuName + ".yml"));
						}
					}

					if (whitelist.contains("item:all")) {
						Arrays.asList(itemDirectory.listFiles()).forEach(File::delete);

						final JsonObject itemFilesJson = json.get("item").getAsJsonObject();
						for (final Entry<String, JsonElement> itemJson : itemFilesJson.entrySet()) {
							final FileConfiguration config = new YamlConfiguration();
							config.loadFromString(itemJson.getValue().getAsString());
							config.save(new File(itemDirectory, itemJson.getKey() + ".yml"));
							logger.info("Downloaded item/" + itemJson.getKey() + ".yml");
						}
					} else {
						for (final String string : whitelist) {
							if (!string.startsWith("item:")) {
								continue;
							}

							final String itemName = string.substring(5);
							if (!json.get("item").getAsJsonObject().has(itemName)) {
								logger.warning("Item with name '" + itemName + "' is in the config sync whitelist, but was not sent by the server.");
								continue;
							}

							final JsonElement itemJson = json.get("item").getAsJsonObject().get(itemName);
							final FileConfiguration config = new YamlConfiguration();
							config.loadFromString(itemJson.getAsString());
							config.save(new File(menuDirectory, itemName + ".yml"));
						}
					}
				} catch (final InvalidConfigurationException e) {
					throw new RuntimeException("The configuration received from the server is invalid", e);
				} catch (final IOException e) {
					e.printStackTrace();
				}

				logger.info("Done! The plugin will now reload.");
				Main.getConfigurationManager().reload();
				Main.server.stop();
				Main.server.start();
			});
		}

	}

}
