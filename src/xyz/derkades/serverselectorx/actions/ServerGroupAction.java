package xyz.derkades.serverselectorx.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import xyz.derkades.serverselectorx.Main;

public class ServerGroupAction extends Action {

	private static final String CHANNEL_NAME = "ssx:servergroup";

	public ServerGroupAction() {
		super("servergroup", true);

		Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Main.getPlugin(), CHANNEL_NAME);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(value);
		player.sendPluginMessage(Main.getPlugin(), CHANNEL_NAME, out.toByteArray());
		return false;
	}

}
