package xyz.derkades.serverselectorx.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.serverselectorx.Main;

public class PlayerCommandAction extends Action {

	public PlayerCommandAction() {
		super("playercommand", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		//Send command 2 ticks later to let the GUI close first (for commands that open a GUI)
		Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
			Bukkit.dispatchCommand(player, PlaceholderUtil.parsePapiPlaceholders(player, value));
		}, 2);
		return false;
	}

}
