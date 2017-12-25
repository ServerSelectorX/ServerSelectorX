package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
		
		String key = request.getParameter("key");
		String serverName = request.getParameter("server");
		String placeholdersJsonString = request.getParameter("data");
		
		Logger logger = Main.getPlugin().getLogger();
		
		if (key == null || serverName == null || placeholdersJsonString == null) {
			logger.warning("Received invalid request from " + request.getRemoteAddr() + ". This may be a hacking attempt.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		String correctKey = Main.getConfigurationManager().getPingersConfig().getString("serverName", "none");
		
		if (correctKey.equals("none")) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			logger.severe("No key has been set for a server with the name " + serverName);
			logger.severe("Please check /plugins/ServerSelectorX/pingers.yml");
			return;
		}
		
		if (!key.equals(correctKey)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid key from " + request.getRemoteAddr());
			logger.warning("This may be a hacking attempt.");
			logger.warning("Provided key: " + key);
			logger.warning("Correct key: " + correctKey);
			return;
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		if (Main.getConfigurationManager().getConfig().getBoolean("log-pinger", true)) {
			logger.info("Recieved message from " + serverName + ": " + placeholdersJsonString);
		}

		Gson gson = new Gson();
		
		Map<String, String> genericSample = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		Map<String, String> placeholders = gson.fromJson(placeholdersJsonString, genericSample.getClass());
		
		Main.PLACEHOLDERS.put(serverName, placeholders);
		
		Main.LAST_INFO_TIME.put(serverName, System.currentTimeMillis());
	}

}
