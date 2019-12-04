package xyz.derkades.serverselectorx.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.ChatColor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ServerPinger {

	private static final String PING_API_URL = "https://api.minetools.eu/ping/%s/%s";

	public interface Server {

		String getIp();
		int getPort();
		boolean isOnline();
		int getOnlinePlayers();
		int getMaximumPlayers();
		String getMotd();
		int getResponseTimeMillis();

	}

	public static class InternalServer implements Server {

		private final String ip;
		private final int port;

		private boolean online;

		private String motd;
		private int onlinePlayers;
		private int maxPlayers;

		public InternalServer(final String ip, final int port, final int timeout) {
			this.ip = ip;
			this.port = port;

			try (Socket socket = new Socket(ip, port)) {
				socket.setSoTimeout(timeout);

				final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				final DataInputStream in = new DataInputStream(socket.getInputStream());

				out.write(0xFE);

				int b;
				final StringBuffer str = new StringBuffer();
				while ((b = in.read()) != -1) {
					if (b != 0 && b > 16 && b != 255 && b != 23 && b != 24) {
						str.append((char) b);
					}
				}

				final String[] data = str.toString().split(ChatColor.COLOR_CHAR + "");

				this.motd = data[0];
				this.onlinePlayers = Integer.parseInt(data[1]);
				this.maxPlayers = Integer.parseInt(data[2]);
			} catch (final UnknownHostException e) {
				this.online = false;
			} catch (final InterruptedIOException e) {
				this.online = false;
			} catch (final IOException e) {
				this.online = false;
			}
		}

		@Override
		public String getIp() {
			return this.ip;
		}

		@Override
		public int getPort() {
			return this.port;
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
			return this.maxPlayers;
		}

		@Override
		public String getMotd() {
			return this.motd;
		}

		@Override
		public int getResponseTimeMillis() {
			return 0;
		}

	}

	public static class ExternalServer implements Server {

		private final String ip;
		private final int port;

		private boolean online;

		private int onlinePlayers;
		private int maximumPlayers;
		private String motd;
		private int responseTimeMillis;

		public ExternalServer(final String ip, final int port) throws PingException {
			this.ip = ip;
			this.port = port;

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

			final JsonParser parser = new JsonParser();
			final JsonObject json = parser.parse(jsonString).getAsJsonObject();

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
		public String getIp() {
			return this.ip;
		}

		@Override
		public int getPort() {
			return this.port;
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

	public static class PingException extends RuntimeException {

		public PingException(final String message) {
			super(message);
		}

		public PingException(final Exception exception) {
			super(exception);
		}

		private static final long serialVersionUID = 1L;

	}

}
