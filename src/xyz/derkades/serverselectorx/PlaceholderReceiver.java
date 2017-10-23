package xyz.derkades.serverselectorx;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class PlaceholderReceiver implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) return;
		
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		short len = in.readShort();
		byte[] msgbytes = new byte[len];
		in.readFully(msgbytes);

		if (subchannel.equals("PlayerCount")) {
			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
			try {
				String serverName = msgin.readUTF();
				String placeholder = msgin.readUTF();
				String output = msgin.readUTF();
				
				Map<String, String> placeholders;
				
				if (Main.PLACEHOLDERS.containsKey(serverName)) {
					placeholders = Main.PLACEHOLDERS.get(serverName);
				} else {
					placeholders = new HashMap<>();
				}
				
				placeholders.put(placeholder, output);
				
				Main.PLACEHOLDERS.put(serverName, placeholders);
				Main.LAST_INFO_TIME.put(serverName, System.currentTimeMillis());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
