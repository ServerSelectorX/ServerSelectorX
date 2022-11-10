package xyz.derkades.serverselectorx.conditional.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Conditions {

	private static final Map<String, Condition> CONDITIONS_BY_TYPE;

	static {
		Condition[] conditions = new Condition[] {
				new CurrentServerCondition(),
				new EffectCondition(),
				new InWorldCondition(),
				new OpenMenuCondition(),
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

	public static @Nullable Condition getConditionByType(@NotNull String type) {
		return CONDITIONS_BY_TYPE.get(type);
	}

}
