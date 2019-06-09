package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PlaceholderReceiver extends HttpServlet {

	private static final long serialVersionUID = -7682997363243721686L;

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		final String key = request.getParameter("key");
		final String placeholdersJsonString = request.getParameter("data");

		final Logger logger = Main.getPlugin().getLogger();

		if (key == null || placeholdersJsonString == null) {
			logger.warning("Received invalid request from " + request.getRemoteAddr() + ". This may be a hacking attempt.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final FileConfiguration config = Main.getConfigurationManager().getServersConfig();

		String serverName = null;

		for (final String server : config.getKeys(false)) {
			final String correctKey = config.getString(server);
			if (key.equals(correctKey)) {
				serverName = server;
			}
		}

		// If key matched with no servers it is incorrect
		if (serverName == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid key from " + request.getRemoteAddr());
			logger.warning("If you configured everything correctly, this may be a hacking attempt.");
			logger.warning("Provided key: " + key);
			return;
		}

		response.setStatus(HttpServletResponse.SC_OK);
		if (Main.getConfigurationManager().getGlobalConfig().getBoolean("log-pinger", true)) {
			logger.info("Recieved message from " + serverName + ": " + placeholdersJsonString);
		}

		final Gson gson = new Gson();

		final Map<String, String> genericSample = new HashMap<>();

		@SuppressWarnings("unchecked")
		final
		Map<String, String> placeholders = gson.fromJson(placeholdersJsonString, genericSample.getClass());

		Main.PLACEHOLDERS.put(serverName, placeholders);

		Main.LAST_INFO_TIME.put(serverName, System.currentTimeMillis());
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (request.getRequestURI().equalsIgnoreCase("/config")) {
			response.setContentType("text/json");

			final Map<Object, Object> json = new HashMap<>();

			json.put("global", Main.getConfigurationManager().getGlobalConfig().saveToString());

			json.put("servers", Main.getConfigurationManager().getServersConfig().saveToString());

			final Map<Object, Object> menuFiles = new HashMap<>();
			for (final Entry<String, FileConfiguration> menuFile : Main.getConfigurationManager().getAllMenus().entrySet()) {
				menuFiles.put(menuFile.getKey(), menuFile.getValue().saveToString());
			}
			json.put("menu", menuFiles);

			response.getOutputStream().println(gson.toJson(json));
		} else {
			response.setContentType("text/json");

			final Map<Object, Object> map = new HashMap<>();
			map.put("version", Main.getPlugin().getDescription().getVersion());
			map.put("api_version", 1);
			map.put("server_count", Main.LAST_INFO_TIME.keySet().size());
			final Map<Object, Object> servers = new HashMap<>();
			for (final String serverName : Main.LAST_INFO_TIME.keySet()) {
				final Map<Object, Object> serverInfo = new HashMap<>();
				serverInfo.put("lastPingTime", Main.LAST_INFO_TIME.get(serverName));
				serverInfo.put("online", Main.isOnline(serverName));
				serverInfo.put("placeholders", Main.PLACEHOLDERS.get(serverName));
				servers.put(serverName, serverInfo);
			}
			map.put("servers", servers);

			final String json = gson.toJson(map);
			response.getOutputStream().println(json);
		}
	}

}
