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

import xyz.derkades.derkutils.caching.Cache;

public class ServerPinger {
	
	private static int CACHE_TIME = 5;
	
	public static interface Server {
		
		public String getIp();
		public int getPort();
		public boolean isOnline();
		public int getOnlinePlayers();
		public int getMaximumPlayers();
		public String getMotd();
		public int getResponseTimeMillis();
		
	}
	
	public static class InternalServer implements Server {

		private String ip;
		private int port;
		
		private boolean online;
		
		private String motd;
		private int onlinePlayers;
		private int maxPlayers;
		
		public InternalServer(String ip, int port, int timeout) {
			this.ip = ip;
			this.port = port;
			
			String id = ip + port;			
			
			//Ping server if not cached
			if (Cache.getCachedObject(id + "online") == null ||
					Cache.getCachedObject(id + "motd") == null ||
					Cache.getCachedObject(id + "onlinePlayers") == null ||
					Cache.getCachedObject(id + "maxPlayers") == null){
			
				try (Socket socket = new Socket(ip, port)){
					socket.setSoTimeout(timeout);
	
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					DataInputStream in = new DataInputStream(socket.getInputStream());
	
					out.write(0xFE);
	
					int b;
					StringBuffer str = new StringBuffer();
					while ((b = in.read()) != -1) {
						if (b != 0 && b > 16 && b != 255 && b != 23 && b != 24) {
							str.append((char) b);
						}
					}
	
					String[] data = str.toString().split(ChatColor.COLOR_CHAR + "");
	
					this.motd = data[0];
					this.onlinePlayers = Integer.parseInt(data[1]);
					this.maxPlayers = Integer.parseInt(data[2]);
				} catch (UnknownHostException e) {
					online = false;
				} catch (InterruptedIOException e){
					online = false;
				} catch (IOException e){
					online = false;
				}
				
				Cache.addCachedObject(id + "online", online, CACHE_TIME);
				Cache.addCachedObject(id + "motd", motd, CACHE_TIME);
				Cache.addCachedObject(id + "onlinePlayers", onlinePlayers, CACHE_TIME);
				Cache.addCachedObject(id + "maxPlayers", maxPlayers, CACHE_TIME);
			} else {
				online = (boolean) Cache.getCachedObject(id + "online");

				motd = (String) Cache.getCachedObject(id + "motd");
				onlinePlayers = (int) Cache.getCachedObject(id + "onlinePlayers");
				maxPlayers = (int) Cache.getCachedObject(id + "maxPlayers");
			}
		}
		
		@Override
		public String getIp() {
			return ip;
		}

		@Override
		public int getPort() {
			return port;
		}

		@Override
		public boolean isOnline() {
			return online;
		}

		@Override
		public int getOnlinePlayers() {
			return onlinePlayers;
		}

		@Override
		public int getMaximumPlayers() {
			return maxPlayers;
		}

		@Override
		public String getMotd() {
			return motd;
		}

		@Override
		public int getResponseTimeMillis() {
			return 0;
		}
		
	}
	
	public static class ExternalServer implements Server {
		
		private String ip;
		private int port;
		
		private boolean online;
		
		private int onlinePlayers;
		private int maximumPlayers;
		private String motd;
		private int responseTimeMillis;
		
		public ExternalServer(String ip, int port) throws PingException {
			this.ip = ip;
			this.port = port;

			String jsonString;
			Object cache = Cache.getCachedObject(ip + port + "json");
			
			if (cache != null) {
				jsonString = (String) cache;
			} else {
				try {
					String url = String.format("https://api.minetools.eu/ping/%s/%s", ip, port);
		
					HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
		
					connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();
		
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					
					in.close();
					
					if (connection.getResponseCode() != 200) {
						throw new PingException("Error while pinging API, HTTP error code " + connection.getResponseCode());
					}
					
					jsonString = response.toString();
					Cache.addCachedObject(ip + port + "json", jsonString, CACHE_TIME);
				} catch (IOException e) {
					throw new PingException(e);
				}
			}
			
			JsonParser parser = new JsonParser();
			JsonObject json = parser.parse(jsonString).getAsJsonObject();
			
			if (json.has("error")) {
				online = false;
			} else {
				online = true;
				JsonObject players = json.get("players").getAsJsonObject();
				onlinePlayers = players.get("online").getAsInt();
				maximumPlayers = players.get("max").getAsInt();
				motd = json.get("description").getAsString();
				responseTimeMillis = (int) json.get("latency").getAsDouble();
			}
		}
		
		@Override
		public String getIp() {
			return ip;
		}
		
		@Override
		public int getPort() {
			return port;
		}

		@Override
		public boolean isOnline() {
			return online;
		}

		@Override
		public int getOnlinePlayers() {
			if (!online) {
				throw new UnsupportedOperationException("Can't get online players of a server that is offline.");
			}
			
			return onlinePlayers;
		}

		@Override
		public int getMaximumPlayers() {
			if (!online) {
				throw new UnsupportedOperationException("Can't get maximum players of a server that is offline.");
			}
			
			return maximumPlayers;
		}

		@Override
		public String getMotd() {
			if (!online) {
				throw new UnsupportedOperationException("Can't get motd of a server that is offline.");
			}
			
			return motd;
		}

		@Override
		public int getResponseTimeMillis() {
			if (!online) {
				throw new UnsupportedOperationException("Can't get response time of a server that is offline.");
			}
			
			return responseTimeMillis;
		}

	}
	
	public static class PingException extends RuntimeException {

		public PingException(String message) {
			super(message);
		}
		
		public PingException(Exception exception) {
			super(exception);
		}

		private static final long serialVersionUID = 5694501675795361821L;
		
	}

}
