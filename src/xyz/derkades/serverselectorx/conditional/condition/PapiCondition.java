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
		String comparisonMode = (String) options.getOrDefault("placeholder-comparison", "equals");

		if (name.contains("%")) {
			throw new InvalidConfigurationException("Placeholder name must not contain percentage symbols");
		}

		String actualValue = PlaceholderUtil.parsePapiPlaceholders(player, "%" + name + "%");
		Objects.requireNonNull(actualValue, "Placeholder value returned by PlaceholderAPI is null! This probably means the expansion or plugin that added the placeholder is broken (not an SSX bug).");

		if (comparisonMode.equals("equals")) {
			Object expectedValueO = options.get("placeholder-value");
			String expectedValue;
			if (expectedValueO instanceof String) {
				expectedValue = (String) expectedValueO;
			} else if (expectedValueO instanceof Integer || expectedValueO instanceof Long) {
				expectedValue = String.valueOf(expectedValueO);
			} else {
				throw new InvalidConfigurationException("When using 'placeholder-comparison: true', placeholder-value must be a string or an integer.");
			}
			return expectedValue.equals(actualValue);
		}

		try {
			if (comparisonMode.equals("less") || comparisonMode.equals("more")) {
				if (!(options.get("placeholder-value") instanceof Number)) {
					throw new InvalidConfigurationException("A conditional section for placeholder '" + name + "' is configured with an expected value that is a string, not an number.");
				}
				double actualValueD = Double.parseDouble(actualValue);
				Number expectedValue = (Number) options.get("placeholder-value");
				if (!Double.isFinite(actualValueD)) {
					throw new InvalidConfigurationException("Placeholder '" + name + "' has value '" + actualValue + "' which is not a valid finite floating point number");
				}
				return comparisonMode.equals("less") && actualValueD < expectedValue.doubleValue() ||
						comparisonMode.equals("more") && actualValueD > expectedValue.doubleValue();
			}
		} catch (NumberFormatException e) {
			throw new InvalidConfigurationException("Used placeholder-comparison less/more but the actual or configured placeholder value cannot be parsed as a number.");
		}

		throw new InvalidConfigurationException("Invalid placeholder-comparison mode, it should be equals, less or more");
	}

}
