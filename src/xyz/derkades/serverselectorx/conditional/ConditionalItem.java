package xyz.derkades.serverselectorx.conditional;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.serverselectorx.conditional.condition.Condition;

import java.util.List;
import java.util.Map;

public class ConditionalItem {

	private static @Nullable Map<String, Object> matchSection(Player player, List<Map<?, ?>> conditionalsList) throws InvalidConfigurationException {
		for (Map<?, ?> genericMap : conditionalsList) {
			Map<String, Object> map = (Map<String, Object>) genericMap;
			if (!map.containsKey("type")) {
				throw new InvalidConfigurationException("Missing 'type' option for a conditional");
			}
			String type = (String) map.get("type");
			Condition condition = Condition.getConditionByType(type);
			if (condition.isTrue(player, map)) {
				return map;
			}
		}
		return null;
	}

	public static ItemStack buildItem(Player player, ConfigurationSection section) throws InvalidConfigurationException {
		Map<String, Object> matchedSection = null;

		if (section.contains("conditional")) {
			matchedSection = matchSection(player, section.getMapList("conditional"));
		}
	}

}
