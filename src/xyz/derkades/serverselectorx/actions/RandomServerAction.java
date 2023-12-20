package xyz.derkades.serverselectorx.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.ServerSelectorX;
import xyz.derkades.serverselectorx.placeholders.Server;

public class RandomServerAction extends Action {

	public RandomServerAction() {
		super("randomserver", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final List<String> serverNames = Arrays.asList(value.split(":"));

		Collections.shuffle(serverNames);

		for (final String serverName : serverNames) {
			final Server server = Server.getServer(serverName);
			if (!server.isOnline()) {
				continue;
			}

			if (server.getOnlinePlayers() >= server.getMaximumPlayers()) {
				continue;
			}

			ServerSelectorX.teleportPlayerToServer(player, serverName);
			return false;
		}

		return false;
	}

}
