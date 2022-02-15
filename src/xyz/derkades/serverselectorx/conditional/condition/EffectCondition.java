package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

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
		return player.hasPotionEffect(effectType);
	}
}
