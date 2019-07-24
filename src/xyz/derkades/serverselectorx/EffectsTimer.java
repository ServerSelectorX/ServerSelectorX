package xyz.derkades.serverselectorx;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class EffectsTimer extends BukkitRunnable {

	private static final int TIMER_INTERVAL = 10*20;
	private static final int EFFECT_DURATION = 12*20;
	
	private final OfflinePlayer offline;
	
	public EffectsTimer(final OfflinePlayer player) {
		this.offline = player;
		
		this.runTaskTimer(Main.getPlugin(), 0, TIMER_INTERVAL);
	}
	
	@Override
	public void run() {
		if (!this.offline.isOnline()) {
			this.cancel();
			return;
		}
		
		final Player player = (Player) this.offline;
		final FileConfiguration config = Main.getConfigurationManager().getGlobalConfig();
		
		if (config.getBoolean("speed-on-join", false)) {
			final int amplifier = config.getInt("speed-amplifier", 3);

			if (config.getBoolean("show-particles")) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, amplifier, true));
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, amplifier, true, false));
			}
		}
		
		if (config.getBoolean("hide-self-on-join", false)) {
			if (config.getBoolean("show-particles")) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, EFFECT_DURATION, 0, true));
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, EFFECT_DURATION, 0, true, false));
			}
		}
	}

}
