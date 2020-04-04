package xyz.derkades.serverselectorx.utils;

import me.dilley.MineStat;

public class MinestatPinger implements ServerPinger {
	
	private boolean isOnline;
	private int online;
	private int max;
	private String motd;
	private int ping;

	public MinestatPinger(final String ip, final int port, final int timeout) {
		final MineStat ms = new MineStat(ip, port);
		ms.setTimeout(timeout);
		if (ms.isServerUp()) {
			this.isOnline = true;
			this.online = Integer.parseInt(ms.getCurrentPlayers());
			this.max = Integer.parseInt(ms.getMaximumPlayers());
			this.motd = ms.getMotd();
			this.ping = (int) ms.getLatency();
		} else {
			this.isOnline = false;
		}
	}

	@Override
	public boolean isOnline() {
		return this.isOnline;
	}

	@Override
	public int getOnlinePlayers() {
		return this.online;
	}

	@Override
	public int getMaximumPlayers() {
		return this.max;
	}

	@Override
	public String getMotd() {
		return this.motd;
	}

	@Override
	public int getResponseTimeMillis() {
		return this.ping;
	}

}
