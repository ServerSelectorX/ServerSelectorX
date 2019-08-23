package xyz.derkades.serverselectorx.actions;

import java.util.Map;

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
		final Map<String, FileConfiguration> menus = Main.getConfigurationManager().getMenus();
		if (!menus.containsKey(value)) {
			player.sendMessage("A menu with the name '" + value + "' does not exist");
			return true;
		}
		
		final FileConfiguration config = menus.get(value);
		
		new Menu(player, config, value).open();
		return false;
	}

}
