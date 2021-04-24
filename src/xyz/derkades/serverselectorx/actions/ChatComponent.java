package xyz.derkades.serverselectorx.actions;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import xyz.derkades.derkutils.bukkit.Chat;
import xyz.derkades.serverselectorx.Main;

public class ChatComponent extends Action {

	public ChatComponent() {
		super("chatcomponent", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		Main.getPlugin().getLogger().warning("The chatcomponent action is deprecated and will be removed in a future release. Please switch to 'advmessage'.");

		final FileConfiguration config = Main.getConfigurationManager().getChatcomponentsConfiguration();

		if (config.contains(value)) {
			player.sendMessage("Error: chat components config does not contain message with name '" + value + "'");
			return true;
		}

		player.spigot().sendMessage(Chat.toComponent(config, value));
		return false;
	}


}
