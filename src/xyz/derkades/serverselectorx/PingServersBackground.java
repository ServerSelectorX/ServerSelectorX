package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.serverselectorx.utils.HolographicPinger;
import xyz.derkades.serverselectorx.utils.JamietePinger;
import xyz.derkades.serverselectorx.utils.MinestatPinger;
import xyz.derkades.serverselectorx.utils.MinetoolsPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger;

public class PingServersBackground extends BukkitRunnable {

	@Override
	public void run() {
		while(true) {
			try {
				for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
					// Ignore if config failed to load
					if (config == null || config.getConfigurationSection("menu") == null) {
						Main.getPlugin().getLogger().warning("Config is not loaded, stopping server ping thread.");
						Main.getPlugin().getLogger().warning("There is probably a YAML syntax error in the menu configuration file. It may also be caused by using the /reload command, please avoid using this command.");
						Main.getPlugin().getLogger().warning("Restart the server after fixing this issue.");
						return;
					}
					
					for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
						// Don't overload the CPU or the API
						try {
							Thread.sleep(1000);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}

						final ConfigurationSection section = config.getConfigurationSection("menu." + key);

						if (!section.getBoolean("ping-server", false)) {
							debug("Skipping, item " + key + " server pinging disabled. ");
							continue;
						}

						final String ip = section.getString("ip");
						final int port = section.getInt("port");
						final int timeout = section.getInt("ping-timeout", 100);
						final String serverId = ip + port;

						debug(String.format("Pinging item %s with address %s:%s", key, ip, port));

						ServerPinger pinger = null;

						switch (Main.getPlugin().getConfig().getString("ping-api", "minestat")) {
						case "minetools":
							pinger = new MinetoolsPinger(ip, port);
						case "jamiete":
							pinger = new JamietePinger(ip, port, timeout);
						case "minestat":
							pinger = new MinestatPinger(ip, port, timeout);
						case "hd":
							pinger = new HolographicPinger(ip, port, timeout);
						}
						
						if (pinger == null) {
							Main.getPlugin().getLogger().warning("Invalid ping-api set in config.yml");
							return;
						}

						debug("Online: " + pinger.isOnline());

						final Map<String, Object> serverInfo = new HashMap<>();
						serverInfo.put("isOnline", pinger.isOnline());
						if (pinger.isOnline()) {
							serverInfo.put("online", pinger.getOnlinePlayers());
							serverInfo.put("max", pinger.getMaximumPlayers());
							serverInfo.put("motd", pinger.getMotd());
							serverInfo.put("ping", pinger.getResponseTimeMillis());
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

	private void debug(final String message) {
		if (Main.getPlugin().getConfig().getBoolean("ping-debug", false)) {
			Main.getPlugin().getLogger().info("[Server Pinging] " + message);
		}
	}

}
