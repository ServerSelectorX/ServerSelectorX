package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.mitch528.sockets.events.MessageReceivedEvent;
import com.mitch528.sockets.events.MessageReceivedEventListener;

public class PlaceholderReceiver implements MessageReceivedEventListener {

	@Override
	public void messageReceived(MessageReceivedEvent event) {
		Main.getPlugin().getLogger().info("Recieved message: " + event.getMessage());
		Gson gson = new Gson();
		
		Map<String, String> genericSample = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		Map<String, String> placeholders = gson.fromJson(event.getMessage(), genericSample.getClass());
		
		String server = placeholders.remove("server");
		Main.PLACEHOLDERS.put(server, placeholders);
	}

}
