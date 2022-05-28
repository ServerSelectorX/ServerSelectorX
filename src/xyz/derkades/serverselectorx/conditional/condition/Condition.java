package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class Condition {

	private final @NotNull String type;

	Condition(final @NotNull String type) {
		this.type = type;
	}

	public @NotNull String getType() {
		return type;
	}

	public abstract boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException;

}
