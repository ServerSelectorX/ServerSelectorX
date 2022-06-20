package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.util.Map;

public class ServerOnlineCondition extends Condition {

	ServerOnlineCondition() {
		super("server-online");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Option 'server-name' missing, this is required to use the 'server-online' condition");
		}

		String serverName = (String) options.get("server-name");
		Server server = Server.getServer(serverName);

		return server.isOnline();
	}
}
