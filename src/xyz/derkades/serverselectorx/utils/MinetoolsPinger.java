package xyz.derkades.serverselectorx.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MinetoolsPinger implements ServerPinger {
	
	private static final String PING_API_URL = "https://api.minetools.eu/ping/%s/%s";

	private boolean online;

	private int onlinePlayers;
	private int maximumPlayers;
	private String motd;
	private int responseTimeMillis;

	public MinetoolsPinger(final String ip, final int port) throws PingException {
		String jsonString;

		try {
			final String url = String.format(PING_API_URL, ip, port);

			final HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			final StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();

			if (connection.getResponseCode() != 200) {
				throw new PingException("Error while pinging API, HTTP error code " + connection.getResponseCode());
			}

			jsonString = response.toString();
		} catch (final IOException e) {
			throw new PingException(e);
		}

		final JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

		if (json.has("error")) {
			this.online = false;
		} else {
			this.online = true;
			final JsonObject players = json.get("players").getAsJsonObject();
			this.onlinePlayers = players.get("online").getAsInt();
			this.maximumPlayers = players.get("max").getAsInt();
			this.motd = json.get("description").getAsString();
			this.responseTimeMillis = (int) json.get("latency").getAsDouble();
		}
	}

	@Override
	public boolean isOnline() {
		return this.online;
	}

	@Override
	public int getOnlinePlayers() {
		return this.onlinePlayers;
	}

	@Override
	public int getMaximumPlayers() {
		if (!this.online) {
			throw new UnsupportedOperationException("Can't get maximum players of a server that is offline.");
		}

		return this.maximumPlayers;
	}

	@Override
	public String getMotd() {
		if (!this.online) {
			throw new UnsupportedOperationException("Can't get motd of a server that is offline.");
		}

		return this.motd;
	}

	@Override
	public int getResponseTimeMillis() {
		if (!this.online) {
			throw new UnsupportedOperationException("Can't get response time of a server that is offline.");
		}

		return this.responseTimeMillis;
	}
	
}
