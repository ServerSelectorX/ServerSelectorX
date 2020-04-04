package xyz.derkades.serverselectorx.utils;

public interface ServerPinger {

	boolean isOnline();
	int getOnlinePlayers();
	int getMaximumPlayers();
	String getMotd();
	int getResponseTimeMillis();

}
