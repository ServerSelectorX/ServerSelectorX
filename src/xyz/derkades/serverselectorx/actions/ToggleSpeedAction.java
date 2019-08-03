package xyz.derkades.serverselectorx.actions;

import java.util.UUID;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.effects.Effects;

public class ToggleSpeedAction extends Action {

	public ToggleSpeedAction() {
		super("togglespeed", false);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final UUID uuid = player.getUniqueId();
		if (Effects.SPEED.contains(uuid)) {
			Effects.SPEED.remove(uuid);
		} else {
			Effects.SPEED.add(uuid);
		}
		return false;
	}

}
