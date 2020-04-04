package xyz.derkades.serverselectorx.utils;

public interface ServerPinger {

	String getIp();
	int getPort();
	boolean isOnline();
	int getOnlinePlayers();
	int getMaximumPlayers();
	String getMotd();
	int getResponseTimeMillis();

}
