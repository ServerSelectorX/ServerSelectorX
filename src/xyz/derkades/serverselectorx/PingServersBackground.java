package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.utils.ServerPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger.Server;

public class PingServersBackground {

	public static void startPinging() {
		while(true) {
			for (FileConfiguration config : Main.getConfigurationManager().getAll()) {
				for (final String key : config.getConfigurationSection("menu").getKeys(false)) {				
					final ConfigurationSection section = config.getConfigurationSection("menu." + key);
					
					String action = section.getString("action");
					
					if (!action.startsWith("srv:")) {
						continue;
					}
					
					if (!section.getBoolean("ping-server", false)) {
						continue;
					}
					
					String serverName = action.substring(4);

					String ip = section.getString("ip");
					int port = section.getInt("port");

					Server server;
					
					if (Main.getPlugin().getConfig().getBoolean("external-query", true)) {
						server = new ServerPinger.ExternalServer(ip, port);
					} else {
						int timeout = section.getInt("ping-timeout", 100);
						server = new ServerPinger.InternalServer(ip, port, timeout);
					}
					
					Map<String, Object> serverInfo = new HashMap<>();
					serverInfo.put("isOnline", server.isOnline());
					if (server.isOnline()) {
						serverInfo.put("online", server.getOnlinePlayers());
						serverInfo.put("max", server.getMaximumPlayers());
						serverInfo.put("motd", server.getMotd());
						serverInfo.put("ping", server.getResponseTimeMillis());
					}
					
					Main.SERVER_PLACEHOLDERS.put(serverName, serverInfo);
					
					// Don't spam the API
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
				}
			}
		}
	}
	
}
