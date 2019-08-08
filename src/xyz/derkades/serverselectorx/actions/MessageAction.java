package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;

public class MessageAction extends Action {

	public MessageAction() {
		super("message", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		player.sendMessage(PlaceholderUtil.parsePapiPlaceholders(player, Colors.parseColors(value)));
		return false;
	}

}
