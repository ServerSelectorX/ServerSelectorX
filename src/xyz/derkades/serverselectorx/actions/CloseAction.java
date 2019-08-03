package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

public class CloseAction extends Action {

	public CloseAction() {
		super("close", false);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		return false;
	}

}
