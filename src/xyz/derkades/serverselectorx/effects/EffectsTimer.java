package xyz.derkades.serverselectorx.effects;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.derkades.serverselectorx.Main;

public class EffectsTimer extends BukkitRunnable {

	private static final int TIMER_INTERVAL = 10*20;
	private static final int EFFECT_DURATION = 12*20;
	
	public EffectsTimer() {
		this.runTaskTimer(Main.getPlugin(), 0, TIMER_INTERVAL);
	}
	
	@Override
	public void run() {
		final FileConfiguration config = Main.getConfigurationManager().getGlobalConfig();
		
		for (final UUID uuid : Effects.SPEED) {
			final Player player = Bukkit.getPlayer(uuid);
			if (player == null) {
				Main.getPlugin().getLogger().warning("Player with uuid " + uuid + " is not online");
				continue;
			}
			
			final int amplifier = config.getInt("speed-amplifier", 3);
	
			if (config.getBoolean("show-particles")) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, amplifier, true));
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, amplifier, true, false));
			}
		}
			
		for (final UUID uuid : Effects.SPEED) {
			final Player player = Bukkit.getPlayer(uuid);
			if (player == null) {
				Main.getPlugin().getLogger().warning("Player with uuid " + uuid + " is not online");
				continue;
			}

			if (config.getBoolean("show-particles")) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, EFFECT_DURATION, 0, true));
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, EFFECT_DURATION, 0, true, false));
			}
		}
	}

}
