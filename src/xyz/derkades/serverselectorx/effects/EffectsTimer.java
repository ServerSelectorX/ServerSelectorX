package xyz.derkades.serverselectorx.effects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.serverselectorx.Main;

public class EffectsTimer extends BukkitRunnable {

	private static final int TIMER_INTERVAL = 10;

	public EffectsTimer() {
		runTaskTimer(Main.getPlugin(), 0, TIMER_INTERVAL);
	}

	@Override
	public void run() {
		final FileConfiguration config = Main.getConfigurationManager().getGlobalConfig();

		for (final Player player : Bukkit.getOnlinePlayers()) {
			if (Effects.SPEED.contains(player.getUniqueId())) {
				final int amplifier = config.getInt("speed-amplifier", 3);

				if (config.getBoolean("show-particles")) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true));
				} else {
					player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true, false));
				}
			} else {
				player.removePotionEffect(PotionEffectType.SPEED);
			}
		}

		for (final Player player : Bukkit.getOnlinePlayers()) {
			if (Effects.INVIS.contains(player.getUniqueId())) {
				if (config.getBoolean("show-particles")) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true));
				} else {
					player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
				}
			} else {
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
			}
		}
	}

}
