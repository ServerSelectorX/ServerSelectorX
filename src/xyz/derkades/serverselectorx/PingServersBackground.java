package xyz.derkades.serverselectorx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import xyz.derkades.derkutils.caching.Cache;
import xyz.derkades.serverselectorx.utils.ServerPinger;

public class PingServersBackground extends BukkitRunnable implements PluginMessageListener {

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
				
				String action = section.getString("action");
				
				if (!action.startsWith("srv:")) {
					continue;
				}
				
				String serverName = action.substring(4);
				
				//Send outgoing message to BungeeCord. BungeeCord will then send a message back, which is handled in the listener down below.
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("PlayerCount");
				out.writeUTF(serverName);
				Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null); //We don't care about which player the message is sent from
				player.sendPluginMessage(Main.getPlugin(), "BungeeCord", out.toByteArray());
				
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
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		if (subchannel.equals("PlayerCount")) {
			String server = in.readUTF();
			int playercount = in.readInt();
			
			Cache.addCachedObject("playersonline" + server, playercount);
		}
	}
	
	private void debug(String msg) {
		Main.getPlugin().getLogger().info("[debug] " + msg);
	}

}
