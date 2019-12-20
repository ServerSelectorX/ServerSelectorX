package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;

public class ServerAction extends Action {

	public ServerAction() {
		super("server", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		Main.teleportPlayerToServer(player, value);
		return false;
	}

}
