package xyz.derkades.serverselectorx.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.Main;

public class GetFile extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final FileConfiguration api = Main.getConfigurationManager().getApiConfiguration();

		if (!api.getBoolean("files-api")) {
			response.getWriter().println("Files API disabled");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		response.setStatus(HttpServletResponse.SC_OK);
		final Path file = Paths.get(request.getParameter("file"));
		final String type = Files.probeContentType(file);
		response.setContentType(type != null ? type : "application/octet-stream");
		Files.copy(file, response.getOutputStream());
	}

}
