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
		if (!options.containsKey("placeholder-name")) {
			throw new InvalidConfigurationException("Missing required option 'placeholder-name' (placeholder name, no %)");
		}

		if (!options.containsKey("placeholder-value")) {
			throw new InvalidConfigurationException("Missing required option 'placeholder-value' (expected placeholder value)");
		}

		String name = (String) options.get("placeholder-name");
		String expectedValue = (String) options.get("placeholder-value");
		String comparisonMode = (String) options.getOrDefault("placeholder-comparison", "equals");

		if (name.contains("%")) {
			throw new InvalidConfigurationException("Placeholder name must not contain percentage symbols");
		}

		String actualValue = PlaceholderUtil.parsePapiPlaceholders(player, "%" + name + "%");
		Objects.requireNonNull(actualValue, "Placeholder value returned by PlaceholderAPI is null! This probably means the expansion or plugin that added the placeholder is broken (not an SSX bug).");

		if (comparisonMode.equals("equals")) {
			return expectedValue.equals(actualValue);
		}

		try {
			if (comparisonMode.equals("less") || comparisonMode.equals("more")) {
				double actualValueD = Double.parseDouble(actualValue);
				double expectedValueD = Double.parseDouble(expectedValue);
				if (!Double.isFinite(actualValueD)) {
					throw new InvalidConfigurationException("Placeholder '" + name + "' has value '" + actualValue + "' which is not a valid finite floating point number");
				}
				if (!Double.isFinite(expectedValueD)) {
					throw new InvalidConfigurationException("A conditional section for placeholder '" + name + "' is configured with expected value '" + expectedValue + "' which is not a valid finite floating point number");
				}
				return comparisonMode.equals("less") && Double.parseDouble(actualValue) < Double.parseDouble(expectedValue) ||
						comparisonMode.equals("more") && Double.parseDouble(actualValue) > Double.parseDouble(expectedValue);
			}
		} catch (NumberFormatException e) {
			throw new InvalidConfigurationException("Used placeholder-comparison less/more but the actual or configured placeholder value cannot be parsed as a number.");
		}

		throw new InvalidConfigurationException("Invalid placeholder-comparison mode, it should be equals, less or more");
	}

}
