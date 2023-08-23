package xyz.derkades.serverselectorx.utils;

import xyz.derkades.serverselectorx.Main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PingTask implements Runnable {

	public static final Map<String, ServerPinger> SERVER_INFO = new HashMap<>();
	private final PingManager pingManager;
	private Iterator<String> serverNameIterator = null;

	PingTask(PingManager pingManager) {
		this.pingManager = pingManager;
	}

	@Override
	public void run() {
		if (this.serverNameIterator == null || !this.serverNameIterator.hasNext()) {
			this.serverNameIterator = pingManager.getServerNames().iterator();
			return;
		}

		final String serverName = this.serverNameIterator.next();

		final ServerPinger pinger = this.pingManager.getServerPinger(serverName);
		if (pinger == null) {
			if (Main.getPlugin().getConfig().getBoolean("ping-debug")) {
				Main.getPlugin().getLogger().info("Server ip for " + serverName + " not yet known");
			}
			return;
		}

		if (Main.getPlugin().getConfig().getBoolean("ping-debug")) {
			Main.getPlugin().getLogger().info("Server pinger: pinging " + pinger.ip + ":" + pinger.port);
			Main.getPlugin().getLogger().info(String.format("Server pinger: online=%s, players=%s",
					pinger.isOnline(), pinger.getOnlinePlayers()));
		}

		pinger.ping();
	}

}
