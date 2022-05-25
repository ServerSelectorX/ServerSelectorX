package xyz.derkades.serverselectorx.actions;

import org.bukkit.entity.Player;
import xyz.derkades.serverselectorx.http.GetPostJoinCommand;

public class PostJoinCommandAction extends Action {

	public PostJoinCommandAction() {
		super("postjoincommand", true);
	}

	@Override
	public boolean apply(Player player, String value) {
		GetPostJoinCommand.setCommand(player.getUniqueId(), value);
		return false;
	}

}
