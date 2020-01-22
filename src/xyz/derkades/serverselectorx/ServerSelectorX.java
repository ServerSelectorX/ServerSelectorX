package xyz.derkades.serverselectorx;

import java.util.List;

import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.placeholders.Server;

public class ServerSelectorX {

	public static void registerAction(final Action action) {
		Action.ACTIONS.add(action);
	}
	
	public static boolean runAction(Player player, String actionString) {
		return Action.runAction(player, actionString);
	}
	
	public static boolean runActions(Player player, List<String> actionStrings) {
		return Action.runActions(player, actionStrings);
	}
	
	public static Server getServer(String name) {
		return Server.getServer(name);
	}

}
