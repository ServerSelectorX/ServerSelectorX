package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import java.util.Map;

public class PermissionCondition extends Condition {

	PermissionCondition() {
		super("permission");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("permission")) {
			throw new InvalidConfigurationException("Missing required option: 'permission' (the permission node to check)");
		}

		String permissionNode = (String) options.get("permissions");
		boolean shouldHavePermission = (boolean) options.getOrDefault("has-permission", true);
		return player.hasPermission(permissionNode) == shouldHavePermission;
	}
}
