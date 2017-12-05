package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.mitch528.sockets.events.MessageReceivedEvent;
import com.mitch528.sockets.events.MessageReceivedEventListener;

public class PlaceholderReceiver implements MessageReceivedEventListener {

	private String serverName;
	
	public PlaceholderReceiver(String serverName) {
		this.serverName = serverName;
	}
	
	@Override
	public void messageReceived(MessageReceivedEvent event) {
		if (Main.getConfigurationManager().getConfig().getBoolean("log-pinger", true)) {
			Main.getPlugin().getLogger().info("Recieved message from " + serverName + ": " + event.getMessage());
		}
		
		Gson gson = new Gson();
		
		Map<String, String> genericSample = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		Map<String, String> placeholders = gson.fromJson(event.getMessage(), genericSample.getClass());
		
		Main.PLACEHOLDERS.put(serverName, placeholders);
		
		Main.LAST_INFO_TIME.put(serverName, System.currentTimeMillis());
	}

}
