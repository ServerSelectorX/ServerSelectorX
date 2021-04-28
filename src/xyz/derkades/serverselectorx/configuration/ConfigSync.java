package xyz.derkades.serverselectorx.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletResponse;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.placeholders.Server;

public class ConfigSync {

	private final Logger logger = Main.getPlugin().getLogger();

	public ConfigSync() {
		final FileConfiguration configSync = Main.getConfigurationManager().getSyncConfiguration();

		if (!configSync.getBoolean("enabled", false)) {
			return;
		}

		final long interval = configSync.getInt("interval") * 60 * 20;

		// Run 1 second after server startup, then every hour
		Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), this::sync, 20, interval);
	}

    private static String encode(final String string) {
        try {
            return URLEncoder.encode(string, StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBaseUrl(final String method) {
    	final FileConfiguration configSync = Main.getConfigurationManager().getSyncConfiguration();
		return "http://" + configSync.getString("address") +
				(method.equals("") ? "" : "/" + method);
    }

	private boolean testConnectivity() {
		try {
			final HttpURLConnection conn = (HttpURLConnection) new URL(getBaseUrl("listfiles") + "?dir=.").openConnection();
			conn.setConnectTimeout(1000);
			conn.connect();
			if (conn.getResponseCode() == HttpServletResponse.SC_OK) {
				return true;
			} else if (conn.getResponseCode() == HttpServletResponse.SC_FORBIDDEN) {
				this.logger.warning("Failed to sync files, is files-api enabled on the other server?");
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
			this.logger.warning("URL: " + getBaseUrl(""));
			return false;
		}
	}

	private void addFilesInDirectory(final String directory, final List<String> files) throws IOException {
		if (directory.endsWith("/")) {
			this.logger.warning("Skipped directory '" + directory + "', directories should not end with a slash.");
			return;
		}

		final URL url = new URL(getBaseUrl("listfiles") + "?dir=" + encode(directory));
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		if (connection.getResponseCode() != 200) {
			this.logger.warning("Skipped directory '" + directory + "', received non-200 HTTP status.");
			this.logger.warning("Status: " + connection.getResponseCode());
			return;
		}

		try (final Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			JsonParser.parseReader(reader).getAsJsonArray().forEach((e) -> files.add(e.getAsString()));
		}
	}

	public List<String> getFilesToSync() {
		final FileConfiguration configSync = Main.getConfigurationManager().getSyncConfiguration();

		final List<String> files = new ArrayList<>();

		// Add all files from the 'files' option
		configSync.getStringList("files").stream().map(s -> Main.getPlugin().getDataFolder().getPath() + "/" + s).forEach(files::add);

		// Add all files from the 'directories' option
		for (final String dir : configSync.getStringList("directories")) {
			try {
				addFilesInDirectory(dir, files);
			} catch (final IOException e) {
				this.logger.warning("An error occured while trying to get a list of files in the directory " + dir);
				e.printStackTrace();
			}
		}

		// Remove excluded files
		configSync.getStringList("exclude").forEach(files::remove);

		return files;
	}

	private InputStream getFileContent(final String file) throws IOException {
		return new URL(getBaseUrl("getfile") + "?file=" + encode(file)).openConnection().getInputStream();
	}

	public void sync() {
		final FileConfiguration configSync = Main.getConfigurationManager().getSyncConfiguration();

		this.logger.info("Starting config sync..");

		// Verify that the address is in the correct format
		try {
			new URL("http://" + configSync.getString("address"));
		} catch (final MalformedURLException e) {
			this.logger.severe("The address you entered seems to be incorrectly formatted.");
			this.logger.severe("It must be formatted like this: 173.45.16.208:8888");
			return;
		}

		if (!this.testConnectivity()) {
			return;
		}

		final File dataFolder = Main.getPlugin().getDataFolder(); // for convenience

		if (configSync.getBoolean("delete", false)) {
			final File[] toDelete = new File[] {
					new File(dataFolder, "item"),
					new File(dataFolder, "command"),
					new File(dataFolder, "menu"),
			};
			for (final File dir : toDelete) {
				try {
					Files.delete(dir.toPath());
				} catch (final IOException e) {
					this.logger.warning("Failed to delete directory" + dir.getPath());
				}
			}
		}

		for (final String fileName : this.getFilesToSync()) {
			try (InputStream input = ConfigSync.this.getFileContent(fileName)){
				final File file = new File(fileName);
				file.getParentFile().mkdirs();
				Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				this.logger.warning("An error occured while trying sync file " + fileName);
				e.printStackTrace();
				continue;
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

		// Clear server status cache to remove any old server names
		Server.clear();

		Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
			final List<String> commands = configSync.getStringList("after-sync-commands");
			if (!commands.isEmpty()) {
				this.logger.info("Running after-sync-commands");
				commands.forEach((c) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c));
			}
		});
	}

}
