package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.InvisibilityToggle;

public class ToggleHideOthersAction extends Action {

	public ToggleHideOthersAction() {
		super("togglehideothers", false);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		if (InvisibilityToggle.hasHiddenOthers(player)) {
			InvisibilityToggle.showOthers(player);
		} else {
			InvisibilityToggle.hideOthers(player);
		}
		return false;
	}

}
