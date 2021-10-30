package xyz.derkades.serverselectorx.http;

import org.apache.commons.lang3.Validate;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Root extends HttpHandler {

	@SuppressWarnings("unchecked")
	@Override
	public void service(Request request, Response response) throws Exception {
		switch (request.getMethod().getMethodString()) {
			case "GET":
				response.setContentType("text/json");

				final Map<Object, Object> map = new HashMap<>();
				map.put("version", Main.getPlugin().getDescription().getVersion());
				map.put("servers", Server.getServers());
				final String json = Main.GSON.toJson(map);
				response.getWriter().write(json);
				break;
			case "POST":
				final String serverName = request.getParameter("server");
				final String placeholdersJsonString = request.getParameter("data");

				final Logger logger = Main.getPlugin().getLogger();

				if (serverName.equals("")) {
					logger.warning("Ignoring request with empty server name from " + request.getRemoteAddr());
					logger.warning("Set the server name in the connector configuration file.");
					return;
				}

				final Map<String, Object> receivedPlaceholders = Main.GSON.fromJson(placeholdersJsonString, Map.class);

				final Map<String, Placeholder> parsedPlaceholders = new HashMap<>();

				// If the code below doesn't make any sense to you, you should read the API documentation:
				// https://github.com/ServerSelectorX/ServerSelectorX/wiki/ServerSelectorX-Premium-API

				receivedPlaceholders.forEach((k, v) -> {
					if (v instanceof String) {
						// If the placeholder value received is a plain string, it is a global
						// placeholder and the value received is the placeholder value.
						parsedPlaceholders.put(k, new GlobalPlaceholder(k, (String) v));
					} else if (v instanceof Map) {
						// If the placeholder value received is a map with UUIDs and strings,
						// it is a player-specific placeholder. The map contains placeholder
						// values for each player. Every map entry is added as a unique
						// player specific placeholder.
						parsedPlaceholders.put(k, new PlayerPlaceholder(k, ((Map<String, String>) v).entrySet().stream()
								.collect(Collectors.toMap(e -> UUID.fromString(e.getKey()), Map.Entry::getValue))));
					} else {
						Main.getPlugin().getLogger().warning("Invalid placeholder value format (" + k + "; " + v + ")");
					}
				});

				final Server server = Server.getServer(serverName);
				server.updatePlaceholders(parsedPlaceholders);
				break;
			default:
				response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
		}
		Validate.isTrue(request.getMethod() == Method.GET, "Must use GET method");
	}

}
