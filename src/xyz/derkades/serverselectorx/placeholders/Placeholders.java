package xyz.derkades.serverselectorx.placeholders;

import java.util.List;

import org.bukkit.entity.Player;

public interface Placeholders {
	
	public String parsePlaceholders(Player player, String string);
	
	public List<String> parsePlaceholders(Player player, List<String> text);

}
