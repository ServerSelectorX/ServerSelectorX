package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;

public class PlaceholderReceiver extends HttpServlet {
	
	private static final long serialVersionUID = -7682997363243721686L;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		String key = request.getParameter("key");
		String placeholdersJsonString = request.getParameter("data");
		
		Logger logger = Main.getPlugin().getLogger();
		
		if (key == null || placeholdersJsonString == null) {
			logger.warning("Received invalid request from " + request.getRemoteAddr() + ". This may be a hacking attempt.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		FileConfiguration config = Main.getConfigurationManager().getServersConfig();
		
		String serverName = null;
		
		for (String server : config.getKeys(false)) {
			String correctKey = config.getString(server);
			if (key.equals(correctKey)) {
				serverName = server;
			}
		}
		
		// If key matched with no servers it is incorrect
		if (serverName == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.warning("Received request with invalid key from " + request.getRemoteAddr());
			logger.warning("If you configured everything correctly, this may be a hacking attempt.");
			logger.warning("Provided key: " + key);
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
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.getWriter().print("Congratulations, you typed an address into your browser. Go tell your mom, she'll be proud of you.");
	}

}
