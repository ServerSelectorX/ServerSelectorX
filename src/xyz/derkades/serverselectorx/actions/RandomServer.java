package xyz.derkades.serverselectorx.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.placeholders.Server;

public class RandomServer extends Action {

	public RandomServer() {
		super("randomserver", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final List<String> serverNames = Arrays.asList(value.split(":"));

		Collections.shuffle(serverNames);

		for (final String serverName : serverNames) {
			final Server server = Server.getServer(serverName);
			if (!server.isOnline()) {
				System.out.println("[randomserver - debug] Skipping " + serverName + ", the server is offline.");
				continue;
			}

			if (server.getOnlinePlayers() >= server.getMaximumPlayers()) {
				System.out.println("[randomserver - debug] Skipping " + serverName + ", player count is too high.");
				continue;
			}

			System.out.println("[randomserver - debug] Teleporting to " + serverName);
			Main.teleportPlayerToServer(player, serverName);
			return false;
		}

		System.out.println("[randomserver - debug] No servers available.");
		return false;
	}

}
