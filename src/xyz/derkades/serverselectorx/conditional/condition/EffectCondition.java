package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xyz.derkades.serverselectorx.Main;

import java.util.Map;

public class EffectCondition extends Condition {

	EffectCondition() {
		super("has-effect");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("effect-type")) {
			throw new InvalidConfigurationException("Missing required option for conditional: 'effect-type'");
		}

		String effectTypeStr = (String) options.get("effect-type");
		PotionEffectType effectType = PotionEffectType.getByName(effectTypeStr);
		if (effectType == null) {
			Main.getPlugin().getLogger().warning(
					String.format("Skipped effect condition for %s, effect type %s is invalid", player.getName(), effectTypeStr));
			return false;
		}

		final PotionEffect activeEffect = player.getPotionEffect(effectType);

		if (activeEffect == null) {
			return false;
		}

		if (!options.containsKey("effect-minimum-strength")) {
			return true;
		}

		int minimumStrength = (int) options.get("effect-minimum-strength");
		// Bukkit amplifier is 0 when displayed strength is 1
		return activeEffect.getAmplifier() + 1 >= minimumStrength;
	}
}
