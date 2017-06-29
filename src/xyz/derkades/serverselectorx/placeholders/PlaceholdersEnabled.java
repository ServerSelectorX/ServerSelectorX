package xyz.derkades.serverselectorx.placeholders;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

public class PlaceholdersEnabled extends Placeholders {

	@Override
	public String parsePlaceholders(Player player, String string) {
		return PlaceholderAPI.setPlaceholders(player, string);
	}

}
