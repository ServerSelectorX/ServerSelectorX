package xyz.derkades.serverselectorx.utils;

public abstract class ServerPinger {
	
	private final String serverId;
	protected final String ip;
	protected final int port;
	protected final int timeout;

	public ServerPinger(final String serverId, final String ip, final int port, final int timeout) {
		this.serverId = serverId;
		this.ip = ip;
		this.port = port;
		this.timeout = timeout;
	}
	
	public String getServerId() {
		return this.serverId;
	}

	abstract public void ping();
	abstract public boolean isOnline();
	abstract public int getOnlinePlayers();
	abstract public int getMaximumPlayers();
	abstract public String getMotd();

}
