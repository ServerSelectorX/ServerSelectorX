package xyz.derkades.serverselectorx.placeholders;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import xyz.derkades.serverselectorx.Main;

public class PlayerPlaceholder extends Placeholder {

	private final Map<UUID, String> values;

	public PlayerPlaceholder(final String key, final Map<UUID, String> values) {
		super(key);
		this.values = Objects.requireNonNull(values, "Placeholder values map is null");
	}

	public Map<UUID, String> getValues() {
		return this.values;
	}

	public String getValue(final UUID uuid) {
		return this.values.get(Objects.requireNonNull(uuid, "Specified UUID is null"));
	}

	public String getValue(final OfflinePlayer player) {
		if (!this.values.containsKey(player.getUniqueId())) {
			return Main.getConfigurationManager().getMiscConfiguration().getString("placeholders.player-missing", "...");
		}

		return this.values.get(player.getUniqueId());
	}

}
