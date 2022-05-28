package xyz.derkades.serverselectorx.conditional.condition;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Conditions {

	private static final Map<String, Condition> CONDITIONS_BY_TYPE;

	static {
		Condition[] conditions = new Condition[] {
				new CurrentServerCondition(),
				new EffectCondition(),
				new PapiCondition(),
				new PermissionCondition(),
				new ServerPlaceholderCondition(),
				new ServerOnlineCondition(),
		};

		CONDITIONS_BY_TYPE = new HashMap<>(conditions.length);

		for (Condition condition : conditions) {
			CONDITIONS_BY_TYPE.put(condition.getType(), condition);
		}
	}

	public static @NotNull Condition getConditionByType(@NotNull String type) {
		return Objects.requireNonNull(CONDITIONS_BY_TYPE.get(type), "No condition registered with type: " + type);
	}

}
