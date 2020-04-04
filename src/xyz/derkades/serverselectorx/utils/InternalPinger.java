package xyz.derkades.serverselectorx.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.bukkit.ChatColor;

public class InternalPinger implements ServerPinger {
	
	private final String ip;
	private final int port;

	private boolean online;

	private String motd;
	private int onlinePlayers;
	private int maxPlayers;

	public InternalPinger(final String ip, final int port, final int timeout) {
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
