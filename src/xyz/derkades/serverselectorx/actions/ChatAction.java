package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;

public class ChatAction extends Action {

	public ChatAction() {
		super("chat", true);
	}

	@Override
	public boolean apply(Player player, String value) {
		player.chat(value);
		return false;
	}

}
