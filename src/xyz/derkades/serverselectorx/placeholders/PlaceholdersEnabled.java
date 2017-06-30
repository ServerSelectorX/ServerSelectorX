package xyz.derkades.serverselectorx.placeholders;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

public class PlaceholdersEnabled extends Placeholders {

	@Override
	public String parsePlaceholders(Player player, String string) {
		String parsed = PlaceholderAPI.setPlaceholders(player, string);
		System.out.println("[debug] player: " + player.getName());
		System.out.println("[debug] in: " + string);
		System.out.println("[debug] out: " + parsed);
		return parsed;
	}

}
