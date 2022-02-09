package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Condition {

	private static final Map<String, Condition> CONDITIONS_BY_TYPE;

	static {
		Condition[] conditions = new Condition[] {
				new PapiCondition(),
				new ServerStatusCondition(),
		};

		CONDITIONS_BY_TYPE = new HashMap<>(conditions.length);

		for (Condition condition : conditions) {
			CONDITIONS_BY_TYPE.put(condition.type, condition);
		}
	}

	private final @NotNull String type;

	Condition(final @NotNull String type) {
		this.type = type;
	}

	public abstract boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException;

	public static @NotNull Condition getConditionByType(@NotNull String type) {
		return Objects.requireNonNull(CONDITIONS_BY_TYPE.get(type), "No condition registered with type: " + type);
	}

}
