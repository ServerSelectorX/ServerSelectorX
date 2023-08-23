package xyz.derkades.serverselectorx.utils;

import xyz.derkades.serverselectorx.Main;

public abstract class ServerPinger {
	
	private final String serverName;
	protected final String ip;
	protected final int port;
	protected final int timeout;

	protected ServerPinger(final String serverName, final String ip, final int port, final int timeout) {
		this.serverName = serverName;
		this.ip = ip;
		this.port = port;
		this.timeout = timeout;
	}
	
	public String getServerName() {
		return this.serverName;
	}

	abstract public void ping();
	abstract public boolean isOnline();
	abstract public int getOnlinePlayers();
	abstract public int getMaximumPlayers();
	abstract public String getMotd();

	static ServerPinger create(String serverName, String ip, int port) {
		final int timeout = Main.getConfigurationManager().getConfig().getInt("ping-timeout", 1000);
		if (Main.getPlugin().getConfig().getBoolean("external-pinger", false)) {
			return new MinetoolsPinger(serverName, ip, port, timeout);
		} else {
			return new HolographicPinger(serverName, ip, port, timeout);
		}
	}

}
