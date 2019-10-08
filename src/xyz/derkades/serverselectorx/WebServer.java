package xyz.derkades.serverselectorx;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

public class WebServer {

	private final int port;

	private Server server;

	public WebServer(final int port) {
		this.port = port;
	}

	public void start() {
		this.server = new Server();

        final ServletHandler handler = new ServletHandler();

        handler.addServletWithMapping(WebServlet.class, "/*");

        this.server.setHandler(handler);

        final ServerConnector connector = new ServerConnector(this.server);
        connector.setPort(this.port);
        this.server.addConnector(connector);

		new Thread() {

			@Override
			public void run() {
				try {
					WebServer.this.server.start();
					Main.getPlugin().getLogger().info("Listening on port " + WebServer.this.port);
					WebServer.this.server.join(); //Join with main thread
				} catch (final Exception e) {
					Main.getPlugin().getLogger().severe("An error occured while starting webserver: " + e.getMessage());
				}
			}

		}.start();
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
