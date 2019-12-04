package xyz.derkades.serverselectorx.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.google.gson.JsonParser;

import xyz.derkades.serverselectorx.Main;

public class ConfigSync {

	private String address;
	private ConfigurationSection config;
	private final Logger logger = Main.getPlugin().getLogger();

	public ConfigSync() {
		final ConfigurationSection syncConfig = Main.getConfigurationManager().getSSXConfig().getConfigurationSection("config-sync");

		if (!syncConfig.getBoolean("enabled", false)) {
			return;
		}

		// Run 1 second after server startup, then every hour
		Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), new Task(), 20, 60*60*20);
	}

	private List<String> getFilesInDirectory(final String directory) throws IOException {
		final URL url = new URL("http://" + this.address + "/listfiles?dir=" + directory);
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		final Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		final List<String> files = new ArrayList<>();
		new JsonParser().parse(reader).getAsJsonArray().forEach((e) -> files.add(e.getAsString()));
		return files;
	}

	private List<String> getFilesToSync() {
		final List<String> filesToSync = new ArrayList<>();

		// Add all files from the 'files'  option
		filesToSync.addAll(this.config.getStringList("files"));

		// Add all files from the 'directories' option
		for (final String dir : this.config.getStringList("directories")) {
			try {
				ConfigSync.this.getFilesInDirectory(dir).forEach((s) -> filesToSync.add(dir + "/" + s));
			} catch (final IOException e) {
				this.logger.warning("An error occured while trying to get a list of files in the directory " + dir);
				e.printStackTrace();
			}
		}

		return filesToSync;
	}

	private String getFileContent(final String file) throws IOException {
		// oneliner http request in java, it is so bad but so awesome at the same time.
		return new BufferedReader(new InputStreamReader(new URL("http://" + this.address + "/getfiles?file=" + file).openConnection().getInputStream())).lines().collect(Collectors.joining("\n"));
	}

	private class Task implements Runnable {

		@Override
		public void run() {
			ConfigSync.this.logger.info("Starting config sync");

			ConfigSync.this.config = Main.getConfigurationManager().getSSXConfig().getConfigurationSection("config-sync");
			ConfigSync.this.address = ConfigSync.this.config.getString("address");

			final File dataFolder = Main.getPlugin().getDataFolder(); // for convenience

			ConfigSync.this.logger.info("Deletion is enabled. Deleting directories");
			final File[] toDelete = new File[] {
					new File(dataFolder, "item"),
					new File(dataFolder, "command"),
					new File(dataFolder, "menu"),
			};
			for (final File dir : toDelete) {
				try {
					FileUtils.deleteDirectory(dir);
				} catch (final IOException e) {
					ConfigSync.this.logger.warning("Failed to delete directory" + dir.getPath());
				}
			}

			// Verify that the address is in the correct format
			try {
				new URL("http://" + ConfigSync.this.address);
			} catch (final MalformedURLException e) {
				ConfigSync.this.logger.severe("The address you entered seems to be incorrectly formatted.");
				ConfigSync.this.logger.severe("It must be formatted like this: 173.45.16.208:8888");
				return;
			}

			for (final String fileName : ConfigSync.this.getFilesToSync()) {
				String content;
				try {
					content = ConfigSync.this.getFileContent(fileName);
				} catch (final IOException e) {
					ConfigSync.this.logger.warning("An error occured while trying to get file content for " + fileName);
					e.printStackTrace();
					continue;
				}

				ConfigSync.this.logger.info("Succesfully retrieved content for file " + fileName);

				// Write contents to file

				try {
					final File file = new File(Main.getPlugin().getDataFolder(), fileName);
					FileUtils.writeStringToFile(file, content, "UTF-8");
				} catch (final IOException e) {
					ConfigSync.this.logger.warning("An error occured while writing file " + fileName);
					e.printStackTrace();
					return;
				}
			}

			ConfigSync.this.logger.info("File sync done! The plugin will now reload.");
			Main.getConfigurationManager().reload();
			Main.server.stop();
			Main.server.start();
		}

	}

}
