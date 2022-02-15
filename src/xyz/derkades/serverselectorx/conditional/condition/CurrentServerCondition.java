package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import xyz.derkades.serverselectorx.Main;

import java.util.Map;

public class CurrentServerCondition extends Condition {

	CurrentServerCondition() {
		super("current-server");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Missing required option: 'server-name'");
		}

		FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

		if (!configMisc.contains("server-name")) {
			throw new InvalidConfigurationException("To use the connected section, specify server-name in misc.yml");
		}

		String serverName = (String) options.get("server-name");
		return configMisc.getString("server-name").equals(serverName);
	}
}
