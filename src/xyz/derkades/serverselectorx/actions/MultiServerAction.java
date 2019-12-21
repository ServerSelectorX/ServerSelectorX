package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.placeholders.Server;

public class MultiServerAction extends Action {

	public MultiServerAction() {
		super("multiserver", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String[] serverNames = value.split(":");

		for (final String serverName : serverNames) {
			final Server server = Server.getServer(serverName);

			if (!server.isOnline()) {
				System.out.println("[multiserver - debug] Skipping " + serverName + ", the server is offline.");
				continue;
			}

			if (server.getOnlinePlayers() >= server.getMaximumPlayers()) {
				System.out.println("[multiserver - debug] Skipping " + serverName + ", player count is too high.");
				continue;
			}

			Main.teleportPlayerToServer(player, serverName);
		}

		return false;
	}

}
