package xyz.derkades.serverselectorx;

import xyz.derkades.serverselectorx.actions.Action;

public class ServerSelectorX {

	public static void registerAction(final Action action) {
		Action.ACTIONS.add(action);
	}

}
