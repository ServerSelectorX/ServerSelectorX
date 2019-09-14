package xyz.derkades.serverselectorx.placeholders;

import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

public class PlayerPlaceholder extends Placeholder {

	private final Map<UUID, String> values;

	public PlayerPlaceholder(final String key, final Map<UUID, String> values) {
		super(key);
		this.values = values;
	}

	public Map<UUID, String> getValues() {
		return this.values;
	}

	public String getValue(final UUID uuid) {
		return this.values.get(uuid);
	}

	public String getValue(final OfflinePlayer player) {
		if (!this.values.containsKey(player.getUniqueId()))
			return "0";

		return this.values.get(player.getUniqueId());
	}

}
