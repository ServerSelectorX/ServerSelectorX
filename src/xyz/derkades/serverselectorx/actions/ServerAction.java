package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.placeholders.Server;

public class ServerAction extends Action {

	public ServerAction() {
		super("server", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		// If offline-cancel-connect is turned on and the server is offline, send message and cancel connecting

		if (!Server.getServer(value).isOnline() &&
				Main.getConfigurationManager().getGlobalConfig().getBoolean("offline-cancel-connect", false)) {
			player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("offline-cancel-connect-message", "error")));
		}

		Main.teleportPlayerToServer(player, value);
		return false;
	}

}
