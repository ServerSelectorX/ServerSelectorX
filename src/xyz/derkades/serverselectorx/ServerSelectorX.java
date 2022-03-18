package xyz.derkades.serverselectorx;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerSelectorX {

	public static void registerAction(final @NotNull Action action) {
		Action.ACTIONS.add(action);
	}
	
	public static boolean runAction(@NotNull Player player, @NotNull String actionString) {
		return Action.runAction(player, actionString);
	}
	
	public static boolean runActions(@NotNull Player player, @NotNull List<String> actionStrings) {
		return Action.runActions(player, actionStrings);
	}
	
	public static @NotNull Server getServer(@NotNull String name) {
		return Server.getServer(name);
	}
	
	public static @NotNull Collection<Server> getServers() {
		return Collections.unmodifiableCollection(Server.getServers().values());
	}
	
	public static @NotNull Set<Server> getOnlineServers() {
		return getServers().stream().filter(Server::isOnline).collect(Collectors.toUnmodifiableSet());
	}
	
	public static int getGlobalPlayerCount() {
		return getServers().stream().filter(Server::isOnline).mapToInt(Server::getOnlinePlayers).sum();
	}
	
	public static void teleportPlayerToServer(final @NotNull Player player, final @NotNull String server){
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
