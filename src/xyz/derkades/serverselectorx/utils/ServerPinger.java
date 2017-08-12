package xyz.derkades.serverselectorx.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import net.md_5.bungee.api.ChatColor;

public class ServerPinger {
	
	private static String API_URL = "https://minecraft-api.com/api/ping/";
	
	/**
	 * Pings a server. Thanks to Xexode on the spigot forums for this! (Some modifications have been made)
	 * 
	 * @return A string array containing the following information:
	 *         <ul>
	 *         <li>MOTD</li>
	 *         <li>Players online</li>
	 *         <li>Max player count</li>
	 *         </ul>
	 *         Or null if the server is not reachable within timeout
	 * @throws PingException 
	 */
	public static String[] pingServer(String ip, int port, int timeout) throws PingException {		
		Socket socket = null;
		try {
			socket = new Socket(ip, port);
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

			if (data == null){
				throw new PingException("Data returned == null");
			}
			
			return data;
		} catch (UnknownHostException e) {
			throw new PingException(e.getMessage());
		} catch (InterruptedIOException e){
			throw new PingException(e.getMessage());
		} catch (IOException e){
			throw new PingException(e.getMessage());
		} finally {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				throw new PingException(e.getMessage());
			}
		}
	}
	
	public static final class Server {
		
		private String ip;
		private int port;
		
		private boolean online;
		
		private Client client;
		private MultivaluedMap<String, String> params;
		
		public Server(String ip, int port) throws PingException {
			this.ip = ip;
			this.port = port;
			
			params = new MultivaluedMapImpl();
			params.add("ip", ip); 
			params.add("port", port + "");
			
			client = Client.create();	
		}
		
		public String getIp() {
			return ip;
		}
		
		public int getPort() {
			return port;
		}
		
		public boolean isOnline() throws PingException {
			getOnlinePlayers();
			return online;
		}
		
		public int getOnlinePlayers() throws PingException {
			WebResource resource = client.resource(API_URL + "playeronline.php");
			ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
			int status = response.getStatus();
			if (status == 500) {
				online = false;
				return 0;
			} else if (status == 200) {
				return response.getEntity(Integer.class);
			} else {
				throw new PingException("Can not get online players for " + ip + ":" + port + " (" + status + ")");
			}
		}
		
		public int getMaximumPlayers() throws PingException {
			WebResource resource = client.resource(API_URL + "maxplayer.php");
			ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
			int status = response.getStatus();
			if (status == 500) {
				online = false;
				return 0;
			} else if (status == 200) {
				return response.getEntity(Integer.class);
			} else {
				throw new PingException("Can not get maximum players for " + ip + ":" + port + " (" + status + ")");
			}
		}
		
		public String getMotd() throws PingException {
			WebResource resource = client.resource(API_URL + "motd.php");
			ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
			int status = response.getStatus();
			if (status == 500) {
				online = false;
				return "";
			} else if (status == 200) {
				return response.getEntity(String.class);
			} else {
				throw new PingException("Can not get motd for " + ip + ":" + port + " (" + status + ")");
			}
		}
		
		public int getReponseTimeMillis() throws PingException {
			WebResource resource = client.resource(API_URL + "ping.php");
			ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
			int status = response.getStatus();
			if (status == 500) {
				online = false;
				return 0;
			} else if (status == 200) {
				return response.getEntity(Integer.class);
			} else {
				throw new PingException("Can not get response time for " + ip + ":" + port + " (" + status + ")");
			}
		}
		
	}
	
	public static class PingException extends Exception {

		public PingException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 5694501675795361821L;
		
	}


}
