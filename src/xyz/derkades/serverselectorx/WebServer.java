package xyz.derkades.serverselectorx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

import xyz.derkades.serverselectorx.servlet.GetFile;
import xyz.derkades.serverselectorx.servlet.ListFiles;
import xyz.derkades.serverselectorx.servlet.Players;
import xyz.derkades.serverselectorx.servlet.Root;

public class WebServer {

	private Server server;

	public void start() {
		this.server = new Server();

		final ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		handler.addServlet(GetFile.class, "/getfile");
		handler.addServlet(ListFiles.class, "/listfiles");
		handler.addServlet(Root.class, "/");
		handler.addServlet(Players.class, "/players");


        this.server.setHandler(handler);

        final ServerConnector connector = new ServerConnector(this.server);
        final FileConfiguration configApi = Main.getConfigurationManager().getApiConfiguration();
        connector.setHost(configApi.getString("host", "127.0.0.1"));
        final int port = configApi.getInt("port");
        connector.setPort(port);
        this.server.addConnector(connector);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
        	try {
				WebServer.this.server.start();
				Main.getPlugin().getLogger().info("Listening on port " + port);
			} catch (final Exception e) {
				Main.getPlugin().getLogger().severe("An error occured while starting webserver: " + e.getMessage());
			}
        });
	}

	public void stop() {
		try {
			this.server.setStopAtShutdown(true);
			this.server.stop();
			Main.getPlugin().getLogger().info("Embedded webserver has been stopped.");
		} catch (final Exception e) {
			Main.getPlugin().getLogger().severe("An error occured while stopping webserver: " + e.getMessage());
		}
	}

	boolean isStopped() {
		return this.server.isStopped();
	}

}
