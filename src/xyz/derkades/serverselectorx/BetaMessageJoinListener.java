package xyz.derkades.serverselectorx;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BetaMessageJoinListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void sendAnnoyingMessage(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (player.hasPermission("ssx.reload") && Main.getPlugin().getDescription().getVersion().contains("beta")) {
			player.sendMessage("You are using a beta version of ServerSelectorX. Please update to a stable version as soon as the functionality or bugfix you require is available in a stable release version. " + ChatColor.GRAY + "(only players with the ssx.reload permission will see this message)");
		}
	}

}
