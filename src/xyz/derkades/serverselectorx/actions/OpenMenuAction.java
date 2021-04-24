package xyz.derkades.serverselectorx.actions;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.Menu;

public class OpenMenuAction extends Action {

	public OpenMenuAction() {
		super("openmenu", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final FileConfiguration menu = Main.getConfigurationManager().getMenuConfiguration(value);

		if (menu == null) {
			player.sendMessage("A menu with the name '" + value + "' does not exist");
			return true;
		}

		final UUID uuid = player.getUniqueId();

		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			final Player player2 = Bukkit.getPlayer(uuid);
			if (player2 == null) {
				Main.getPlugin().getLogger().warning("Player " + uuid + " left while running openmenu action");
				return;
			}
			new Menu(player, menu, value);
		}, 0);
		return false;
	}

}
