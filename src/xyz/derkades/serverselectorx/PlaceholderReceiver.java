package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class PlaceholderReceiver /*implements MessageReceivedEventListener*/ extends HttpServlet {
	
	

	/*private String serverName;
	
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
	}*/
	
	private static final long serialVersionUID = -7682997363243721686L;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		String serverName = request.getParameter("server");
		String placeholdersJsonString = request.getParameter("data");
		
		if (serverName == null || placeholdersJsonString == null) {
			Main.getPlugin().getLogger().warning("Recieved invalid request from " + request.getRemoteAddr());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} else {
			response.setStatus(HttpServletResponse.SC_OK);
			if (Main.getConfigurationManager().getConfig().getBoolean("log-pinger", true)) {
				Main.getPlugin().getLogger().info("Recieved message from " + serverName + ": " + placeholdersJsonString);
			}
		}

		Gson gson = new Gson();
		
		Map<String, String> genericSample = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		Map<String, String> placeholders = gson.fromJson(placeholdersJsonString, genericSample.getClass());
		
		Main.PLACEHOLDERS.put(serverName, placeholders);
		
		Main.LAST_INFO_TIME.put(serverName, System.currentTimeMillis());
	}

}
