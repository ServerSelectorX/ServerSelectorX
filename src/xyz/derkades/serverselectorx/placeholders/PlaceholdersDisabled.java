package xyz.derkades.serverselectorx.placeholders;

import java.util.List;

import org.bukkit.entity.Player;

import xyz.derkades.derkutils.bukkit.Colors;

public class PlaceholdersDisabled extends Placeholders {

	@Override
	public String parsePlaceholders(Player player, String string) {
		System.out.println("[debug] placeholderapi not active");
		return Colors.parseColors(string);
	}

	@Override
	public List<String> parsePlaceholders(Player player, List<String> text) {
		return Colors.parseColors(text);
	}

}
