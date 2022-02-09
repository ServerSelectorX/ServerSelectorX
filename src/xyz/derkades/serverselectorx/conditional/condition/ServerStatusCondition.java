package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.util.Map;

public class ServerStatusCondition extends Condition {

	ServerStatusCondition() {
		super("server-online");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Missing option: server-name");
		}

		String serverName = (String) options.get("server-name");
		Server server = Server.getServer(serverName);

		final boolean expectedStatus;
		if (options.containsKey("online")) {
			if (!(options.get("online") instanceof Boolean)) {
				throw new InvalidConfigurationException("Option 'online' must be boolean");
			}
			expectedStatus = (boolean) options.get("online");
		} else {
			expectedStatus = true;
		}

		return server.isOnline() == expectedStatus;
	}
}
