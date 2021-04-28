package xyz.derkades.serverselectorx.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

		final String filePath = request.getParameter("file");

		Objects.requireNonNull(filePath, "File path not specified");

		response.setStatus(HttpServletResponse.SC_OK);
		final Path file = Path.of(filePath);
		final String type = Files.probeContentType(file);
		response.setContentType(type != null ? type : "application/octet-stream");
		Files.copy(file, response.getOutputStream());
	}

}
