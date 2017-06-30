package xyz.derkades.serverselectorx.placeholders;

import org.bukkit.entity.Player;

public class PlaceholdersDisabled extends Placeholders {

	@Override
	public String parsePlaceholders(Player player, String string) {
		System.out.println("[debug] placeholderapi not active");
		return string;
	}

}
