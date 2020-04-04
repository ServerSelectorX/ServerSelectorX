package xyz.derkades.serverselectorx.utils;

import java.io.IOException;

import ch.jamiete.mcping.MinecraftPing;
import ch.jamiete.mcping.MinecraftPingOptions;
import ch.jamiete.mcping.MinecraftPingReply;
import xyz.derkades.serverselectorx.Main;

public class JamietePinger implements ServerPinger {
	
	private boolean online;

	private String motd;
	private int onlinePlayers;
	private int maxPlayers;

	public JamietePinger(final String ip, final int port, final int timeout) {
		try {
			final MinecraftPingReply data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(ip).setPort(port).setTimeout(timeout));
			this.onlinePlayers = data.getPlayers().getOnline();
			this.maxPlayers = data.getPlayers().getMax();
			this.motd = data.getDescription().getText();
			this.online = true;
		} catch (final IOException e) {
			this.online = false;
			
			if (Main.getPlugin().getConfig().getBoolean("ping-debug")) {
				e.printStackTrace();
			}
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

	@Override
	public int getResponseTimeMillis() {
		return 0;
	}

}
