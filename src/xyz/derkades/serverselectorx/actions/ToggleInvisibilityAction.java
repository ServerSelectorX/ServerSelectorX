package xyz.derkades.serverselectorx.actions;

import java.util.UUID;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.effects.Effects;

public class ToggleInvisibilityAction extends Action {

	public ToggleInvisibilityAction() {
		super("toggleinvis", false);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final UUID uuid = player.getUniqueId();
		if (Effects.INVIS.contains(uuid)) {
			Effects.INVIS.remove(uuid);
		} else {
			Effects.INVIS.add(uuid);
		}
		return false;
	}

}
