package xyz.derkades.serverselectorx.utils;

import xyz.derkades.serverselectorx.holographicdisplays_serverpinger.PingResponse;

public class HolographicPinger extends ServerPinger {
	
	public HolographicPinger(final String serverId, final String ip, final int port, final int timeout) {
		super(serverId, ip, port, timeout);
	}

	private boolean online;
	private String motd;
	private int onlinePlayers;
	private int maxPlayers;

	@Override
	public void ping() {
		try {
			final PingResponse response = xyz.derkades.serverselectorx.holographicdisplays_serverpinger.ServerPinger
					.fetchData(this.ip, this.port, this.timeout);
			this.online = response.isOnline();
			this.motd = response.getMotd();
			this.onlinePlayers = response.getOnlinePlayers();
			this.maxPlayers = response.getMaxPlayers();
		} catch (final Exception e) {
			this.online = false;
			e.printStackTrace();
		}
	}

	@Override
	public boolean isOnline() {
		return this.online;
	}

	@Override
	public int getOnlinePlayers() {
		return this.onlinePlayers;
	}

	@Override
	public int getMaximumPlayers() {
		return this.maxPlayers;
	}

	@Override
	public String getMotd() {
		return this.motd;
	}

}
