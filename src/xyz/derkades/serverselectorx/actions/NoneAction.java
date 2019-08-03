package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

public class NoneAction extends Action {

	public NoneAction() {
		super("none", false);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		return false; // False means stay open
	}

}
