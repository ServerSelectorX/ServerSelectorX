package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.placeholders.Server;

public class ServerSelectorX {

	public static void registerAction(final Action action) {
		Action.ACTIONS.add(action);
	}
	
	public static boolean runAction(Player player, String actionString) {
		return Action.runAction(player, actionString);
	}
	
	public static boolean runActions(Player player, List<String> actionStrings) {
		return Action.runActions(player, actionStrings);
	}
	
	public static Server getServer(String name) {
		return Server.getServer(name);
	}
	
	public static List<Server> getServers() {
		return Server.getServers();
	}
	
	public static List<Server> getOnlineServers() {
		return Server.getServers().stream().filter(Server::isOnline).collect(Collectors.toList());
	}
	
	public static int getGlobalPlayerCount() {
		return Server.getServers().stream().filter(Server::isOnline).mapToInt(Server::getOnlinePlayers).sum();
	}
	
	public static void teleportPlayerToServer(final Player player, final String server){
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0) {
			return;
		}

		Cooldown.addCooldown("servertp" + player.getName() + server, 1000);

		// Send message to bungeecord
		try (
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos)
			){

	        dos.writeUTF("Connect");
	        dos.writeUTF(server);
	        player.sendPluginMessage(Main.getPlugin(), "BungeeCord", baos.toByteArray());
		} catch (final IOException e){
			throw new RuntimeException(e);
		}
	}

}
