package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger.Server;

public class PingServersBackground extends Thread {

	@Override
	public void run() {
		while(true) {
			try {
				for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
					for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
						// Don't overload the CPU or the API
						try {
							Thread.sleep(1000);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}

						final ConfigurationSection section = config.getConfigurationSection("menu." + key);

						if (!section.getBoolean("ping-server", false)) {
							continue;
						}

						final String ip = section.getString("ip");
						final int port = section.getInt("port");
						final String serverId = ip + port;

						Server server;

						if (Main.getPlugin().getConfig().getBoolean("external-query", true)) {
							server = new ServerPinger.ExternalServer(ip, port);
						} else {
							final int timeout = section.getInt("ping-timeout", 100);
							server = new ServerPinger.InternalServer(ip, port, timeout);
						}

						final Map<String, Object> serverInfo = new HashMap<>();
						serverInfo.put("isOnline", server.isOnline());
						if (server.isOnline()) {
							serverInfo.put("online", server.getOnlinePlayers());
							serverInfo.put("max", server.getMaximumPlayers());
							serverInfo.put("motd", server.getMotd());
							serverInfo.put("ping", server.getResponseTimeMillis());
						}

						Main.SERVER_PLACEHOLDERS.put(serverId, serverInfo);
					}
				}
			} catch (final Exception e) {
				// So the loop doesn't break if an error occurs
				// Print the error, sleep, try again.
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (final InterruptedException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

}
