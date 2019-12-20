package xyz.derkades.serverselectorx.effects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.derkades.serverselectorx.Main;

public class EffectsOnJoin implements Listener {

	public EffectsOnJoin() {
		Bukkit.getPluginManager().registerEvents(this, Main.getPlugin());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(final PlayerJoinEvent event) {
		final FileConfiguration config = Main.getConfigurationManager().effects;
		final Player player = event.getPlayer();

		for (final String value : config.getStringList("join")) {
			if (!Main.getConfigurationManager().effects.contains("effects." + value)) {
				Main.getPlugin().getLogger().warning("Effect '" + value + "' is not declared in the effects.yml file.");
				Main.getPlugin().getLogger().warning("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Effects");
				return;
			}

			final PotionEffectType effect = PotionEffectType.getByName(value);

			if (effect == null) {
				Main.getPlugin().getLogger().warning("Invalid effect type '" + value + "'.");
				Main.getPlugin().getLogger().warning("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Effects");
				return;
			}

			final int amplifier = Main.getConfigurationManager().effects.getInt("effects." + value);
			player.addPotionEffect(new PotionEffect(effect, Integer.MAX_VALUE, amplifier, true, true));
		}


	}

}
