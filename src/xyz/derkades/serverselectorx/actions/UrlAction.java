package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.Main;

public class UrlAction extends Action {

	public UrlAction() {
		super("url", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String url = value;
		final String message = Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("url-message", "&3&lClick here"));

		player.spigot().sendMessage(
				new ComponentBuilder(message)
				.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
				.create()
				);
		return false;
	}

}
