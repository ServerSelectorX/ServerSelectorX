package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class WebServlet extends HttpServlet {

	private static final long serialVersionUID = -7682997363243721686L;

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		final String password = request.getParameter("password");
		final String server = request.getParameter("server");
		final String placeholdersJsonString = request.getParameter("data");

		final Logger logger = Main.getPlugin().getLogger();

		if (password == null || server == null || placeholdersJsonString == null) {
			logger.warning("Received invalid request from " + request.getRemoteAddr());
			logger.warning("Make sure that you are using the correct version of SSX-Connector");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final FileConfiguration config = Main.getConfigurationManager().getSSXConfig();

		if (config.getString("password", "a").equals(password)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid password from " + request.getRemoteAddr());
			logger.warning("Provided password: " + password);
			return;
		}

		response.setStatus(HttpServletResponse.SC_OK);

		if (Main.getConfigurationManager().getGlobalConfig().getBoolean("log-pinger", true)) {
			logger.info("Recieved message from " + server + ": " + placeholdersJsonString);
		}

		final Gson gson = new Gson();

		final Map<String, Map<String, String>> genericSample = new HashMap<>();

		@SuppressWarnings("unchecked")
		final Map<String, Map<String, String>> receivedPlaceholders = gson.fromJson(placeholdersJsonString, genericSample.getClass());

		final Map<UUID, Map<String, String>> serverPlaceholders = new HashMap<>();

		receivedPlaceholders.forEach((uuidString, placeholders) -> {
			final UUID uuid;
			if (uuidString.equals("global")){
				uuid = null;
			} else {
				uuid = UUID.fromString(uuidString);
			}

			serverPlaceholders.put(uuid, placeholders);
		});

		Main.PLACEHOLDERS.put(server, serverPlaceholders);
		Main.LAST_INFO_TIME.put(server, System.currentTimeMillis());
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType("text/json");

		final String password = request.getParameter("password");

		final Logger logger = Main.getPlugin().getLogger();

		if (password == null) {
			logger.warning("Received invalid request from " + request.getRemoteAddr());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final FileConfiguration config = Main.getConfigurationManager().getSSXConfig();

		if (config.getString("password", "a").equals(password)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid password from " + request.getRemoteAddr());
			logger.warning("Provided password: " + password);
			return;
		}

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (request.getRequestURI().equalsIgnoreCase("/config")) {
			final Map<Object, Object> json = new HashMap<>();

			json.put("global", Main.getConfigurationManager().getGlobalConfig().saveToString());

			final Map<Object, Object> menuFiles = new HashMap<>();
			for (final Entry<String, FileConfiguration> menuFile : Main.getConfigurationManager().getMenus().entrySet()) {
				menuFiles.put(menuFile.getKey(), menuFile.getValue().saveToString());
			}
			json.put("menu", menuFiles);

			final Map<Object, Object> itemFiles = new HashMap<>();
			for (final Entry<String, FileConfiguration> itemFile : Main.getConfigurationManager().getItems().entrySet()) {
				itemFiles.put(itemFile.getKey(), itemFile.getValue().saveToString());
			}
			json.put("item", itemFiles);

			response.getOutputStream().println(gson.toJson(json));
		} else {
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
