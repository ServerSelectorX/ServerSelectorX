package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.ServerSelectorX;
import xyz.derkades.serverselectorx.placeholders.Server;

public class FirstAvailableServerAction extends Action {

	public FirstAvailableServerAction() {
		super("firstavailableserver", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String[] serverNames = value.split(":");

		for (final String serverName : serverNames) {
			final Server server = Server.getServer(serverName);

			if (!server.isOnline()) {
				System.out.println("[firstavailableserver - debug] Skipping " + serverName + ", the server is offline.");
				continue;
			}

			if (server.getOnlinePlayers() >= server.getMaximumPlayers()) {
				System.out.println("[firstavailableserver - debug] Skipping " + serverName + ", player count is too high.");
				continue;
			}

			ServerSelectorX.teleportPlayerToServer(player, serverName);
		}

		return false;
	}

}
