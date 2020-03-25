package xyz.derkades.serverselectorx.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

		final long interval = Main.getConfigurationManager().sync.getInt("interval") * 60 * 20;

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
		return "http://%s" + (method.equals("") ? "" : "/" + method) +
				Main.getConfigurationManager().sync.getString("address") + "?password=" +
				encode(Main.getConfigurationManager().sync.getString("password"));
    }

	private boolean testConnectivity() {
		try {
			final HttpURLConnection conn = (HttpURLConnection) new URL(getBaseUrl("")).openConnection();
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
			this.logger.warning("URL: " + getBaseUrl(""));
			return false;
		}
	}

	private List<String> getFilesInDirectory(final String directory) throws IOException {
		this.logger.info("Listing files in directory " + directory);
		
//		final URL url = new URL(String.format("http://%s/listfiles?password=%s&dir=%s",
//				Main.getConfigurationManager().sync.getString("address"),
//				Main.getConfigurationManager().sync.getString("password"),
//				directory
//				));
		final URL url = new URL(getBaseUrl("listfiles") + "&dir=" + encode(directory));
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		final Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		final List<String> files = new ArrayList<>();
		new JsonParser().parse(reader).getAsJsonArray().forEach((e) -> files.add(e.getAsString()));
		
		for (final String f : files) {
			if (isDirectory(f)) {
				files.addAll(getFilesInDirectory(f));
			}
		}
		
		return files;
	}
	
	private boolean isDirectory(final String path) throws IOException {
//		final URL url = new URL(String.format("http://%s/fileinfo?password=%s&file=%s",
//				Main.getConfigurationManager().sync.getString("address"),
//				Main.getConfigurationManager().sync.getString("password"),
//				path));
		final URL url = new URL(getBaseUrl("fileinfo") + "&file=" + encode(path));
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		final Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return new JsonParser().parse(reader).getAsJsonObject().get("directory").getAsBoolean();
	}

	private List<String> getFilesToSync() {
		final List<String> filesToSync = new ArrayList<>();

		// Add all files from the 'files' option
		filesToSync.addAll(Main.getConfigurationManager().sync.getStringList("files"));

		// Add all files from the 'directories' option
		for (final String dir : Main.getConfigurationManager().sync.getStringList("directories")) {
			try {
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

	private byte[] getFileContent(final String file) throws IOException {
//		return new BufferedReader(new InputStreamReader(new URL(getBaseUrl("getfile") + "&file=" + encode(file)).openConnection().getInputStream()))
//				.lines().collect(Collectors.joining("\n"));
		return new URL(getBaseUrl("getfile") + "&file=" + encode(file)).openConnection().getInputStream().readAllBytes();
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
			byte[] content;
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
//				FileUtils.writeStringToFile(file, content, "UTF-8");
				final OutputStream stream = new FileOutputStream(file);
				stream.write(content);
				stream.flush();
				stream.close();
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
