package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;

import java.util.Map;
import java.util.Objects;

public class PapiCondition extends Condition {

	PapiCondition() {
		super("papi");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("name")) {
			throw new InvalidConfigurationException("Missing required option 'name' (placeholder name, no %)");
		}

		if (!options.containsKey("value")) {
			throw new InvalidConfigurationException("Missing required option 'value' (expected placeholder value)");
		}

		String name = (String) options.get("name");
		String expectedValue = (String) options.get("value");
		String comparisonMode = (String) options.getOrDefault("comparison", "equals");

		if (name.contains("%")) {
			throw new InvalidConfigurationException("Placeholder name must not contain percentage symbols");
		}

		String actualValue = PlaceholderUtil.parsePapiPlaceholders(player, "%" + name + "%");
		Objects.requireNonNull(actualValue, "Placeholder value returned by PlaceholderAPI is null! This probably means the expansion or plugin that added the placeholder is broken (not an SSX bug).");

		return comparisonMode.equals("equals") && expectedValue.equals(actualValue) ||
						comparisonMode.equals("less") && Double.parseDouble(expectedValue) > Double.parseDouble(actualValue) ||
						comparisonMode.equals("more") && Double.parseDouble(expectedValue) < Double.parseDouble(actualValue);
	}

}
