package xyz.derkades.serverselectorx.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;

public class ConsoleCommandAction extends Action {

	public ConsoleCommandAction() {
		super("consolecommand", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String command = value.replace("{player}", player.getName());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Main.PLACEHOLDER_API.parsePlaceholders(player, command));
		return false;
	}

}
