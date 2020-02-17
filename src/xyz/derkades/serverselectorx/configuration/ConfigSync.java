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

import com.google.gson.JsonParser;

import xyz.derkades.serverselectorx.Main;

public class ConfigSync {

	private final Logger logger = Main.getPlugin().getLogger();

	public ConfigSync() {
		if (!Main.getConfigurationManager().sync.getBoolean("enabled", false)) {
			return;
		}

		final long interval = Main.getConfigurationManager().sync.getInt("interval")*60*20;

		// Run 1 second after server startup, then every hour
		Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), this::sync, 20, interval);
	}

	private boolean testConnectivity() {
		final String url = String.format("http://%s?password=%s",
				Main.getConfigurationManager().sync.getString("address"),
				Main.getConfigurationManager().sync.getString("password"));
		try {
			final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(1000);
			conn.connect();
			if (conn.getResponseCode() == 200) {
				return true;
			} else if (conn.getResponseCode() == 401) {
				this.logger.warning("Invalid password");
				return false;
			} else {
				this.logger.warning("Received bad request response code");
				this.logger.warning("This is probably an issue with the plugin");
				this.logger.warning("Make sure that you are using the latest and/or same version everywhere.");
				return false;
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			this.logger.warning("Connection error.");
			this.logger.warning("Is the server down? Is the address correct? Firewall?");
			this.logger.warning("URL: " + url);
			return false;
		}
	}

	private List<String> getFilesInDirectory(final String directory) throws IOException {
		final URL url = new URL(String.format("http://%s/listfiles?password=%s&dir=%s",
				Main.getConfigurationManager().sync.getString("address"),
				Main.getConfigurationManager().sync.getString("password"),
				directory
				));
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		final Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		final List<String> files = new ArrayList<>();
		new JsonParser().parse(reader).getAsJsonArray().forEach((e) -> files.add(e.getAsString()));
		return files;
	}

	private List<String> getFilesToSync() {
		final List<String> filesToSync = new ArrayList<>();

		// Add all files from the 'files' option
		filesToSync.addAll(Main.getConfigurationManager().sync.getStringList("files"));

		// Add all files from the 'directories' option
		for (final String dir : Main.getConfigurationManager().sync.getStringList("directories")) {
			try {
				this.logger.info("Listing files in directory " + dir);
				ConfigSync.this.getFilesInDirectory(dir).forEach((s) -> filesToSync.add(dir + "/" + s));
			} catch (final IOException e) {
				this.logger.warning("An error occured while trying to get a list of files in the directory " + dir);
				e.printStackTrace();
			}
		}
		
		// Remove excluded files
		Main.getConfigurationManager().sync.getStringList("exclude").forEach(filesToSync::remove);
		
		this.logger.info("Files to sync (" + filesToSync.size() + "): ");
		filesToSync.forEach((f) -> this.logger.info(" - " + f));

		return filesToSync;
	}

	private String getFileContent(final String file) throws IOException {
		return new BufferedReader(new InputStreamReader(new URL(String.format("http://%s/getfile?password=%s&file=%s", Main.getConfigurationManager().sync.getString("address"), Main.getConfigurationManager().sync.getString("password"), file)).openConnection().getInputStream())).lines().collect(Collectors.joining("\n")); // sorry lol
	}

	public void sync() {
		this.logger.info("Starting config sync..");
		
		// Verify that the address is in the correct format
		try {
			new URL("http://" + Main.getConfigurationManager().sync.getString("address"));
		} catch (final MalformedURLException e) {
			this.logger.severe("The address you entered seems to be incorrectly formatted.");
			this.logger.severe("It must be formatted like this: 173.45.16.208:8888");
			return;
		}

		if (!this.testConnectivity()) {
			return;
		}

		final File dataFolder = Main.getPlugin().getDataFolder(); // for convenience
		
		if (Main.getConfigurationManager().sync.getBoolean("delete", false)) {
			this.logger.info("Deletion is enabled. Deleting directories");
			final File[] toDelete = new File[] {
					new File(dataFolder, "item"),
					new File(dataFolder, "command"),
					new File(dataFolder, "menu"),
			};
			for (final File dir : toDelete) {
				try {
					FileUtils.deleteDirectory(dir);
					this.logger.info("Deleted directory " + dir);
				} catch (final IOException e) {
					this.logger.warning("Failed to delete directory" + dir.getPath());
				}
			}
		}

		for (final String fileName : this.getFilesToSync()) {
			String content;
			try {
				content = ConfigSync.this.getFileContent(fileName);
			} catch (final IOException e) {
				this.logger.warning("An error occured while trying to get file content for " + fileName);
				e.printStackTrace();
				continue;
			}

			this.logger.info("Succesfully retrieved content for file " + fileName);

			// Write contents to file

			try {
				final File file = new File(Main.getPlugin().getDataFolder(), fileName);
				FileUtils.writeStringToFile(file, content, "UTF-8");
			} catch (final IOException e) {
				this.logger.warning("An error occured while writing file " + fileName);
				e.printStackTrace();
				return;
			}
		}

		this.logger.info("File sync done! The plugin will now reload.");

		try {
			Main.getConfigurationManager().reload();
			this.logger.info("Reload complete.");
		} catch (final IOException e) {
			Main.getPlugin().getLogger().warning("Oh no! There was a syntax error in the config file pulled"
					+ "from the other server. The plugin will probably stop working. For a detailed error "
					+ "report, use /ssx reload on the other server.");
		}
		
		final List<String> commands = Main.getConfigurationManager().sync.getStringList("after-sync-commands");
		if (!commands.isEmpty()) {
			this.logger.info("Running after-sync-commands");
			commands.forEach((c) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c));
		}
	}

}
