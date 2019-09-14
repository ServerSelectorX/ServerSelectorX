package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import xyz.derkades.derkutils.bukkit.PlaceholderUtil.Placeholder;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;

public class WebServlet extends HttpServlet {

	private static final long serialVersionUID = -7682997363243721686L;

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		final String password = request.getParameter("password");
		final String serverName = request.getParameter("server");
		final String placeholdersJsonString = request.getParameter("data");

		final Logger logger = Main.getPlugin().getLogger();

		if (password == null || serverName == null || placeholdersJsonString == null) {
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
			logger.info("Recieved message from " + serverName + ": " + placeholdersJsonString);
		}

		final Gson gson = new Gson();

		final Map<String, Object> receivedPlaceholders = gson.fromJson(placeholdersJsonString, Map.class);

		final List<Placeholder> placeholders = new ArrayList<>();

		final Map<String, String> globalMap = (Map<String, String>) receivedPlaceholders.remove("global");
		globalMap.forEach((k, v) -> placeholders.add(new GlobalPlaceholder(k, v)));

		receivedPlaceholders.forEach((k, v) -> {
			if (v instanceof String) {
				placeholders.add(new GlobalPlaceholder(k, (String) v));
			} else if (v instanceof Map) {
				final Map<UUID, String> values = new HashMap<>();
				((Map<String, String>) v).forEach((k2, v2) -> values.put(UUID.fromString(k2), v2));
				placeholders.add(new PlayerPlaceholder(k, values));
			} else {
				Main.getPlugin().getLogger().warning("Invalid placeholder value format (" + k + "; " + v + ")");
			}
		});

		final Server server = Server.getServer(serverName);
		server.updatePlaceholders(placeholders);
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

			response.getOutputStream().println(new Gson().toJson(json));
		} else if (request.getRequestURI().equalsIgnoreCase("/players")) {
			final Gson gson = new Gson();
			final Map<UUID, String> players = new HashMap<>();
			Bukkit.getOnlinePlayers().forEach((p) -> players.put(p.getUniqueId(), p.getName()));
			response.getOutputStream().println(gson.toJson(players));
		} else {
			final Map<Object, Object> map = new HashMap<>();
			map.put("version", Main.getPlugin().getDescription().getVersion());
			map.put("api_version", 2);
			map.put("servers", Server.getServers());

			final String json = new GsonBuilder().setPrettyPrinting().create().toJson(map);
			response.getOutputStream().println(json);
		}
	}

}
