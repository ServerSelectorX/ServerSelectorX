package xyz.derkades.serverselectorx.actions;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundAction extends Action {

	public SoundAction() {
		super("sound", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		try {
			player.playSound(player.getLocation(), Sound.valueOf(value), 1.0f, 1.0f);
		} catch (final IllegalArgumentException e) {
			player.sendMessage("Invalid sound name");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Sounds");
		}

		return false;
	}

}
