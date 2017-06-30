package xyz.derkades.serverselectorx.placeholders;

import java.util.List;

import org.bukkit.entity.Player;

public abstract class Placeholders {
	
	public abstract String parsePlaceholders(Player player, String string);
	
	public abstract List<String> parsePlaceholders(Player player, List<String> text);

}
