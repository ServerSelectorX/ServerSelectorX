package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import xyz.derkades.serverselectorx.Main;

public class AdvMessageAction extends Action {

	public AdvMessageAction() {
		super("advmessage", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final Component message = MiniMessage.get().deserialize(value);
		Main.adventure().player(player).sendMessage(message);
		return false;
	}

}
