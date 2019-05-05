package xyz.derkades.serverselectorx;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class WebServer {
	
	private int port;
	
	private Server server;
	
	public WebServer(int port) {
		this.port = port;
	}
	
	public void start() {
		server = new Server();
		
        ServletHandler handler = new ServletHandler();

        handler.addServletWithMapping(PlaceholderReceiver.class, "/*");
        
        server.setHandler(handler);

		new Thread() {
			
			@Override
			public void run() {
				try {
					server.start();
					Main.getPlugin().getLogger().info("Server has been started on port " + WebServer.this.port);
					server.join(); //Join with main thread
				} catch (Exception e) {
					Main.getPlugin().getLogger().severe("An error occured while starting server: " + e.getMessage());
				}
			}
			
		}.start();	    
	}
	
	public void stop() {
		try {
			server.setStopAtShutdown(true);
			server.stop();
			Main.getPlugin().getLogger().info("Server has been stopped.");
		} catch (Exception e) {
			Main.getPlugin().getLogger().severe("An error occured while stopping server: " + e.getMessage());
		}
	}

}
