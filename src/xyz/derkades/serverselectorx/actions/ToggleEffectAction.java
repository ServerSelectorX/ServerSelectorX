package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.derkades.serverselectorx.Main;

public class ToggleEffectAction extends Action {

	public ToggleEffectAction() {
		super("toggleeffect", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		if (!Main.getConfigurationManager().effects.contains("effects." + value)) {
			player.sendMessage("Effect '" + value + "' is not declared in the effects.yml file.");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Effects");
			return true;
		}

		final PotionEffectType effect = PotionEffectType.getByName(value);

		if (effect == null) {
			player.sendMessage("Invalid effect type '" + value + "'.");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Effects");
			return true;
		}

		if (player.hasPotionEffect(effect)) {
			player.removePotionEffect(effect);
		} else {
			final int amplifier = Main.getConfigurationManager().effects.getInt("effects." + value);
			player.addPotionEffect(new PotionEffect(effect, Integer.MAX_VALUE, amplifier, true, true));
		}

		return false;
	}

}
