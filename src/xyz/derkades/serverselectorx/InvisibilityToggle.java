package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class InvisibilityToggle {

	public static List<UUID> INVISIBILITY_ON = new ArrayList<>();

	@SuppressWarnings("deprecation")
	public static void showOthers(final Player player) {
		for (final Player online : Bukkit.getOnlinePlayers()) {
			if (online.getUniqueId() == player.getUniqueId()) {
				continue;
			}

			try {
				player.showPlayer(Main.getPlugin(), online);
			} catch (final NoSuchMethodError e) {
				player.showPlayer(online);
			}
		}

		INVISIBILITY_ON.remove(player.getUniqueId());
	}

	@SuppressWarnings("deprecation")
	public static void hideOthers(final Player player) {
		for (final Player online : Bukkit.getOnlinePlayers()) {
			if (online.getUniqueId() == player.getUniqueId()) {
				continue;
			}

			try {
				player.hidePlayer(Main.getPlugin(), online);
			} catch (final NoSuchMethodError e) {
				player.hidePlayer(online);
			}
		}

		INVISIBILITY_ON.add(player.getUniqueId());
	}

	public static boolean hasHiddenOthers(final Player player) {
		return INVISIBILITY_ON.contains(player.getUniqueId());
	}
}
