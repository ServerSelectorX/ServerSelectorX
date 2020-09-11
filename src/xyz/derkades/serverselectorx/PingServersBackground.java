package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.utils.HolographicPinger;
import xyz.derkades.serverselectorx.utils.MinetoolsPinger;
import xyz.derkades.serverselectorx.utils.ServerPinger;

public class PingServersBackground implements Runnable {

	public static final Map<String, ServerPinger> SERVER_INFO = new HashMap<>();
	private static final List<ServerPinger> PING_JOBS = new ArrayList<>();
	private static int pos = 0;
	
	@Override
	public void run() {
		if (PING_JOBS.isEmpty()) {
			reschedule(10*20);
			return;
		}
		
		if (pos >= PING_JOBS.size()) {
			pos = 0;
			reschedule(5*20);
			return;
		}

		PING_JOBS.get(pos++).ping();
		
		reschedule(20);
	}

	public static void generatePingJobs() {
		PING_JOBS.clear();
		SERVER_INFO.clear();
		for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
			// Ignore if config failed to load
			if (config == null || config.getConfigurationSection("menu") == null) {
				Main.getPlugin().getLogger().warning("Config is not loaded, stopping server ping thread.");
				Main.getPlugin().getLogger().warning("There is probably a YAML syntax error in the menu configuration "
						+ "file. It can also be caused by using the /reload command, please avoid using this command.");
				Main.getPlugin().getLogger().warning("Restart the server after fixing this issue.");
				return;
			}
			
			for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
				final ConfigurationSection section = config.getConfigurationSection("menu." + key);

				if (!section.getBoolean("ping-server", false)) {
					continue;
				}

				final String ip = section.getString("ip");
				final int port = section.getInt("port");
				final int timeout = Main.getConfigurationManager().getConfig().getInt("ping-timeout", 1000);
				final String serverId = ip + port;

				final ServerPinger pinger = Main.getPlugin().getConfig().getBoolean("external-pinger", false) ?
						new MinetoolsPinger(serverId, ip, port, timeout) :
						new HolographicPinger(serverId, ip, port, timeout);
				
				PING_JOBS.add(pinger);
				SERVER_INFO.put(serverId, pinger);
			}
		}
	}
	
	private void reschedule(final int delay) {
		Main.pingTask = Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(), this, delay);
	}

}
