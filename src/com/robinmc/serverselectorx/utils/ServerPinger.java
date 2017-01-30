package com.robinmc.serverselectorx.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerPinger {
	
	/**
	 * Pings a server. Huge thanks to Xexode on the spigot forums for this! (Some modifications have been made)
	 * 
	 * @return A string array containing the following information:
	 *         <ul>
	 *         <li>MOTD</li>
	 *         <li>Players online</li>
	 *         <li>Max player count</li>
	 *         </ul>
	 * @throws PingException 
	 */
	public static String[] pingServer(String ip, int port) throws PingException {
		Socket socket = null;
		try {
			socket = new Socket(ip, port);

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

			String[] data = str.toString().split("§");

			return data;
		} catch (UnknownHostException e) {
			throw new PingException(e.getMessage());
		} catch (IOException e) {
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
	
	public static class PingException extends Exception {

		public PingException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 5694501675795361821L;
		
	}


}
