package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class InvisibilityToggle {
	
	public static List<UUID> INVISIBILITY_ON = new ArrayList<>();
	
	public static void showOthers(Player player) {
		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online.getUniqueId() == player.getUniqueId()) {
				continue;
			}
			
			player.showPlayer(Main.getPlugin(), online);
		}
		
		INVISIBILITY_ON.remove(player.getUniqueId());
	}
	
	public static void hideOthers(Player player) {
		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online.getUniqueId() == player.getUniqueId()) {
				continue;
			}
			
			player.hidePlayer(Main.getPlugin(), online);
		}
		
		INVISIBILITY_ON.add(player.getUniqueId());
	}
	
	public static boolean hasHiddenOthers(Player player) {
		return INVISIBILITY_ON.contains(player.getUniqueId());
	}
}
