package xyz.derkades.serverselectorx;

import org.bukkit.configuration.file.FileConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import xyz.derkades.serverselectorx.http.GetFile;
import xyz.derkades.serverselectorx.http.ListFiles;
import xyz.derkades.serverselectorx.http.Players;
import xyz.derkades.serverselectorx.http.Root;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WebServer {

	private HttpServer server;

	public void start() {
		this.server = new HttpServer();
		final FileConfiguration api = Main.getConfigurationManager().getApiConfiguration();
		this.server.addListener(new NetworkListener("Listener", api.getString("host", "127.0.0.1"), api.getInt("port")));
		ServerConfiguration config = server.getServerConfiguration();
		config.addHttpHandler(new GetFile(), "/getfile");
		config.addHttpHandler(new ListFiles(), "/listfiles");
		config.addHttpHandler(new Root());
		config.addHttpHandler(new Players(), "/players");

		try {
			this.server.start();
		} catch (final IOException e) {
			Main.getPlugin().getLogger().severe("An error occured while starting webserver: " + e.getMessage());
		}
	}

	public void stop() {
		try {
			Main.getPlugin().getLogger().info("Stopping embedded webserver...");
			this.server.shutdown().get(5000, TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
			Main.getPlugin().getLogger().severe("An error occured while stopping webserver: " + e.getMessage());
		}
	}

}
