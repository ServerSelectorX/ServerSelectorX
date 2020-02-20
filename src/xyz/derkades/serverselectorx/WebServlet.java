package xyz.derkades.serverselectorx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

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

		final FileConfiguration api = Main.getConfigurationManager().api;

		final String correctPassword = api.getString("password", "a");

		if (!correctPassword.equals(password)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid password from " + request.getRemoteAddr());
			logger.warning(String.format("Provided=['%s'] Correct=['%s'] ", password, correctPassword));
			return;
		}

		response.setStatus(HttpServletResponse.SC_OK);

		if (serverName.equals("")) {
			logger.warning("Ignoring request with empty server name from " + request.getRemoteAddr());
			logger.warning("Set the server name in the connector configuration file.");
			return;
		}

		final Gson gson = new Gson();

		final Map<String, Object> receivedPlaceholders = gson.fromJson(placeholdersJsonString, Map.class);

		final List<Placeholder> parsedPlaceholders = new ArrayList<>();

		// If the code below doesn't make any sense to you, you should read the API documentation:
		// https://github.com/ServerSelectorX/ServerSelectorX/wiki/ServerSelectorX-Premium-API

		receivedPlaceholders.forEach((k, v) -> {
			if (v instanceof String) {
				// If the placeholder value received is a plain string, it is a global
				// placeholder and the value received is the placeholder value.
				parsedPlaceholders.add(new GlobalPlaceholder(k, (String) v));
			} else if (v instanceof Map) {
				// If the placeholder value received is a map with UUIDs and strings,
				// it is a player-specific placeholder. The map contains placeholder
				// values for each player. Every map entry is added as a unique
				// player specific placeholder.
				final Map<UUID, String> values = new HashMap<>();
				((Map<String, String>) v).forEach((k2, v2) -> values.put(UUID.fromString(k2), v2));
				parsedPlaceholders.add(new PlayerPlaceholder(k, values));
			} else {
				Main.getPlugin().getLogger().warning("Invalid placeholder value format (" + k + "; " + v + ")");
			}
		});

		final Server server = Server.getServer(serverName);
		server.updatePlaceholders(parsedPlaceholders);
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String password = request.getParameter("password");

		final Logger logger = Main.getPlugin().getLogger();

		if (password == null) {
			logger.warning("Received invalid request from " + request.getRemoteAddr());
			logger.warning("No password was provided.");
			logger.warning("Request URI: " + request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final FileConfiguration api = Main.getConfigurationManager().api;

		final String correctPassword = api.getString("password", "a");

		if (!correctPassword.equals(password)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid password from " + request.getRemoteAddr());
			logger.warning(String.format("Provided='%s' Correct='%s' ", password, correctPassword));
			return;
		}

		if (request.getRequestURI().equals("/getfile")) {
			final String fileName = request.getParameter("file");

			// Do not allow going outside of the plugin directory for security reasons
			if (api.getBoolean("block-outside-directory", true) && fileName.contains("..")) {
				logger.warning("Received request with dangerous filename from " + request.getRemoteAddr());
				logger.warning("File name: " + fileName);
				logger.warning("The request has been blocked, no need to panic. Maybe do consider looking into where the request came from?");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			final File file = new File(Main.getPlugin().getDataFolder(), fileName);
			final String contents = FileUtils.readFileToString(file, "UTF-8");
			response.getOutputStream().print(contents);
		}

		else if (request.getRequestURI().equals("/listfiles")) {
			final String dirName = request.getParameter("dir");
			// Do not allow going outside of the plugin directory for security reasons
			if (api.getBoolean("block-outside-directory", true) && dirName.contains("..")) {
				logger.warning("Received request with dangerous directory name from " + request.getRemoteAddr());
				logger.warning("Directory name: " + dirName);
				logger.warning("The request has been blocked, no need to panic. Maybe do consider looking into where the request came from?");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			final File dir = new File(Main.getPlugin().getDataFolder(), dirName);
			if (!dir.isDirectory()) {
				logger.warning("Received bad request from " + request.getRemoteAddr());
				logger.warning("Requested to list files in " + dirName + ", but it is not a directory.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			final List<String> fileNames = new ArrayList<>();
			for (final File file : dir.listFiles()) {
				if (!file.isDirectory()) {
					fileNames.add(file.getName());
				}
			}

			response.setContentType("text/json");
			final String json = new GsonBuilder().setPrettyPrinting().create().toJson(fileNames);
			response.getOutputStream().print(json);
		}
		
		else if (request.getRequestURI().equals("/fileinfo")) {
			final String fileName = request.getParameter("file");
			// Do not allow going outside of the plugin directory for security reasons
			if (api.getBoolean("block-outside-directory", true) && fileName.contains("..")) {
				logger.warning("Received request with dangerous directory name from " + request.getRemoteAddr());
				logger.warning("Directory name: " + fileName);
				logger.warning("The request has been blocked, no need to panic. Maybe do consider looking into where the request came from?");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			final File file = new File(fileName);
			
			final Map<String, Boolean> info = new HashMap<>();
			info.put("exists", file.exists());
			info.put("directory", file.isDirectory());
			response.getOutputStream().print(new Gson().toJson(info));
		}

		else if (request.getRequestURI().equals("/players")) {
			final Gson gson = new Gson();
			final Map<UUID, String> players = new HashMap<>();
			Bukkit.getOnlinePlayers().forEach((p) -> players.put(p.getUniqueId(), p.getName()));
			response.getOutputStream().println(gson.toJson(players));
		}

		else if (request.getRequestURI().equals("/")) {
			response.setContentType("text/json");

			final Map<Object, Object> map = new HashMap<>();
			map.put("version", Main.getPlugin().getDescription().getVersion());
			map.put("api_version", 2);
			map.put("servers", Server.getServers());

			final String json = new GsonBuilder().setPrettyPrinting().create().toJson(map);
			response.getOutputStream().println(json);
		}

		else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

}
