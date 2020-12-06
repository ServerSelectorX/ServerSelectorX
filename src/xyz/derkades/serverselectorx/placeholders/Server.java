package xyz.derkades.serverselectorx.placeholders;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import xyz.derkades.serverselectorx.Main;

public class Server {
	
	public static final JsonSerializer<Server> SERIALIZER = (src, typeOfSrc, context) -> {
		final JsonObject json = new JsonObject();
		json.addProperty("name", src.getName());
		json.addProperty("online", src.isOnline());
		if (src.placeholders == null) {
			json.add("placeholders", Server.EMPTY_OBJECT);
		} else {
			json.add("placeholders", Main.GSON.toJsonTree(src.placeholders));
		}
		return json;
	};
	
	private static final JsonObject EMPTY_OBJECT = new JsonObject();
	
//	private static final GlobalPlaceholder ONLINE_0 = new GlobalPlaceholder("online", "0");
//	private static final GlobalPlaceholder MAX_0 = new GlobalPlaceholder("max", "0");
	
//	private static final List<Server> SERVERS = new ArrayList<>();
	private static final Map<String, Server> SERVERS = new HashMap<>();

	private final String name;
	private transient long lastInfoTime = 0;
	private Map<String, Placeholder> placeholders;

	public Server(final String name) {
		if (name == null) {
			throw new NullPointerException("Server name can't be null");
		}

		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public long getTimeSinceLastMessage() {
		return System.currentTimeMillis() - this.lastInfoTime;
	}

	public boolean isOnline() {
		final int timeout = Main.getConfigurationManager().api.getInt("server-offline-timeout", 6000);
		return getTimeSinceLastMessage() < timeout;
	}

	public Collection<Placeholder> getPlaceholders() {
		return this.placeholders.values();
	}
	
	public boolean hasPlaceholder(final String name) {
		return this.placeholders != null && this.placeholders.containsKey(name);
	}

	public Placeholder getPlaceholder(final String name) {
		if (!this.isOnline()) {
			return new GlobalPlaceholder(name, Main.getConfigurationManager().misc.getString("placeholders.offline", "-"));
		}
		
		if (this.hasPlaceholder(name)) {
			return this.placeholders.get(name);
		}

		Main.getPlugin().getLogger().warning("Placeholder " + name + " was requested but not received from the server (" + getName() + ").");
		return new GlobalPlaceholder(name, Main.getConfigurationManager().misc.getString("placeholders.missing", "?"));
	}

	public int getOnlinePlayers() {
		return hasPlaceholder("online") ? Integer.parseInt(((GlobalPlaceholder) getPlaceholder("online")).getValue()) : 0;
	}

	public int getMaximumPlayers() {
		return hasPlaceholder("max") ? Integer.parseInt(((GlobalPlaceholder) getPlaceholder("max")).getValue()) : 0;
	}
	
	public void updatePlaceholders(final Map<String, Placeholder> placeholders) {
		this.placeholders = placeholders;
		this.lastInfoTime = System.currentTimeMillis();
	}

	public static Map<String, Server> getServers() {
		return SERVERS;
	}

	public static Server getServer(final String name) {
		if (name == null) {
			throw new NullPointerException("Name can't be null");
		}
		
		if (SERVERS.containsKey(name)) {
			return SERVERS.get(name);
		} else {
			final Server server = new Server(name);
			SERVERS.put(name, server);
			return server;
		}
	}
	
	public static void clear() {
		SERVERS.clear();
	}

}
