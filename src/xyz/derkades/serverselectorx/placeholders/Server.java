package xyz.derkades.serverselectorx.placeholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.derkades.serverselectorx.Main;

public class Server {

//	private static final List<Server> SERVERS = new ArrayList<>();
	private static final Map<String, Server> SERVERS = new HashMap<>();

	private final String name;
	private transient long lastInfoTime = 0;
	private List<Placeholder> placeholders;

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

	public List<Placeholder> getPlaceholders() {
		return this.placeholders;
	}

	public Placeholder getPlaceholder(final String name) {
		if (!this.isOnline()) {
			return new GlobalPlaceholder(name, "?");
		}
		
		if (getPlaceholders() != null) {
			for (final Placeholder placeholder : getPlaceholders()) {
				if (placeholder.getKey().equals(name)) {
					return placeholder;
				}
			}
		}

		Main.getPlugin().getLogger().warning("Placeholder " + name + " was requested but not received from the server (" + getName() + ").");
		return new GlobalPlaceholder(name, "-");
	}

	public int getOnlinePlayers() {
		return Integer.parseInt(((GlobalPlaceholder) getPlaceholder("online")).getValue());
	}

	public int getMaximumPlayers() {
		return Integer.parseInt(((GlobalPlaceholder) getPlaceholder("max")).getValue());
	}

	public void updatePlaceholders(final List<Placeholder> placeholders) {
		this.lastInfoTime = System.currentTimeMillis();
		this.placeholders = placeholders;
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
