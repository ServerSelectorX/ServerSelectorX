package xyz.derkades.serverselectorx.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;

public class DelayAction extends Action {

	public DelayAction() {
		super("delay", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String timeString = value.split(":")[0];
		final String actionString = value.substring(timeString.length() + 1);

		int delay;
		try {
			delay = Integer.parseInt(timeString);
		} catch (final NumberFormatException e) {
			player.sendMessage("Invalid number " + timeString);
			return false;
		}

		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> Action.runAction(player, actionString), delay);
		return false;
	}

}
