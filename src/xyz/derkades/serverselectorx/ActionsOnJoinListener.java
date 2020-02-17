package xyz.derkades.serverselectorx;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import xyz.derkades.serverselectorx.actions.Action;

public class ActionsOnJoinListener implements Listener {
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(final PlayerJoinEvent event) {
		final List<String> actions = Main.getConfigurationManager().join.getStringList("actions");
		Action.runActions(event.getPlayer(), actions);
	}

}
