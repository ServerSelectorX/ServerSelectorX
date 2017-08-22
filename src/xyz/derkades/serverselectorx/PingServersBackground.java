package xyz.derkades.serverselectorx;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.serverselectorx.utils.ServerPinger;

public class PingServersBackground extends BukkitRunnable {

	private boolean pinging = false;
	
	@Override
	public void run() { //Called every 5 ticks
		if (pinging) {
			/* If servers are still being pinged, just */ return;
		}
		
		pinging = true;
		
		//Start pinging all servers
		
		for (FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {
			for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
				final ConfigurationSection section = config.getConfigurationSection("menu." + key);
				if (section.getBoolean("ping-server", false)) { //If server pinging is enabled for this slot
					String ip = section.getString("ip");
					int port = section.getInt("port");
					
					if (Main.getPlugin().getConfig().getBoolean("external-query", true)){
						new ServerPinger.ExternalServer(ip, port);
					} else {
						int timeout = section.getInt("ping-timeout", 100);
						new ServerPinger.InternalServer(ip, port, timeout);
					}
				}
			}
		}
		
		//Done pinging servers
		pinging = false;
	}

}
