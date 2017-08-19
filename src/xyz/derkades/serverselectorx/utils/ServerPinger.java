package xyz.derkades.serverselectorx.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.ws.rs.core.MultivaluedMap;

import org.bukkit.ChatColor;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import xyz.derkades.derkutils.caching.Cache;

public class ServerPinger {
	
	private static String API_URL = "https://minecraft-api.com/api/ping/";
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
		
		private String id; //Used for caching
		
		private Client client;
		private MultivaluedMap<String, String> params;
		
		public ExternalServer(String ip, int port) throws PingException {
			this.ip = ip;
			this.port = port;
			
			this.id = ip + port;
			
			params = new MultivaluedMapImpl();
			params.add("ip", ip); 
			params.add("port", port + "");
			
			client = Client.create();	
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
			Object cache = Cache.getCachedObject(id + "online");
			
			if (cache == null) {
				WebResource resource = client.resource(API_URL + "ping.php");
				ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
				int status = response.getStatus();
				if (status == 200) {
					Cache.addCachedObject(id + "online", true, CACHE_TIME);
					return true;
				} else if (status == 500) {
					Cache.addCachedObject(id + "online", false, CACHE_TIME);
					return false;
				} else {
					throw new PingException(String.format("Unexpected error while pinging [%s:%s]. HTTP error code %s", ip, port, status));
				}
			} else {
				return (boolean) cache;
			}
		}
		
		@Override
		public int getOnlinePlayers() {
			if (!this.isOnline()) {
				throw new AssertionError("Tried to get online players of a server that is offline.");
			}
			
			Object cache = Cache.getCachedObject(id + "onlinePlayers");
			
			if (cache == null) {
				WebResource resource = client.resource(API_URL + "playeronline.php");
				ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
				int status = response.getStatus();
				if (status == 200) {
					int onlinePlayers = response.getEntity(Integer.class);
					Cache.addCachedObject(id + "onlinePlayers", onlinePlayers, CACHE_TIME);
					return onlinePlayers;
				} else {
					throw new PingException(String.format("Unexpected error while pinging [%s:%s]. HTTP error code %s", ip, port, status));
				}
			} else {
				return (int) cache;
			}
		}
		
		@Override
		public int getMaximumPlayers() {
			if (!this.isOnline()) {
				throw new AssertionError("Tried to get maximum players of a server that is offline.");
			}
			
			Object cache = Cache.getCachedObject(id + "maxPlayers");
			
			if (cache == null) {
				WebResource resource = client.resource(API_URL + "maxplayer.php");
				ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
				int status = response.getStatus();
				if (status == 200) {
					int maxPlayers = response.getEntity(Integer.class);
					Cache.addCachedObject(id + "maxPlayers", maxPlayers, CACHE_TIME);
					return maxPlayers;
				} else {
					throw new PingException(String.format("Unexpected error while pinging [%s:%s]. HTTP error code %s", ip, port, status));
				}
			} else {
				return (int) cache;
			}
		}
		
		@Override
		public String getMotd() {
			
			if (!this.isOnline()) {
				throw new AssertionError("Tried to get motd of a server that is offline.");
			}
			
			Object cache = Cache.getCachedObject(id + "motd");
			
			if (cache == null) {
				WebResource resource = client.resource(API_URL + "motd.php");
				ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
				int status = response.getStatus();
				if (status == 200) {
					String motd = response.getEntity(String.class);
					Cache.addCachedObject(id + "motd", motd, CACHE_TIME);
					return motd;
				} else {
					//throw new AssertionError("Tried to get motd of server that is offline. Error code " + status);
					throw new PingException(String.format("Unexpected error while pinging [%s:%s]. HTTP error code %s", ip, port, status));
				}
			} else {
				return (String) cache;
			}
		}
		
		@Override
		public int getResponseTimeMillis() {
			
			if (!this.isOnline()) {
				throw new AssertionError("Tried to get response time of a server that is offline.");
			}
			
			Object cache = Cache.getCachedObject(id + "ping");
			
			if (cache == null) {
				WebResource resource = client.resource(API_URL + "ping.php");
				ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
				int status = response.getStatus();
				if (status == 200) {
					int ping = response.getEntity(Integer.class);
					Cache.addCachedObject(id + "ping", ping, CACHE_TIME);
					return ping;
				} else {
					throw new PingException(String.format("Unexpected error while pinging [%s:%s]. HTTP error code %s", ip, port, status));
				}
			} else {
				return (int) cache;
			}
		}
		
	}
	
	public static class PingException extends RuntimeException {

		public PingException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 5694501675795361821L;
		
	}

}
